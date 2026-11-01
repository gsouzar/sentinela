package br.edu.sentinela;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.naming.directory.InitialDirContext;
import javax.net.ssl.*;
import org.springframework.stereotype.Component;

/** Passive HEAD of verified origins. Address is validated once and pinned to the socket. */
@Component
public class PublicSiteProbe {
 public record Headers(int code,List<Map<String,String>> findings){}
 public record Result(String status,long responseMs,int httpCode,Long tlsExpires,List<Map<String,String>> findings,String message){}
 public static String origin(String raw){
  String normalized;
  try { normalized=new UrlAnalyzer().analyze(raw.strip()).url(); }
  catch(IllegalArgumentException invalid){throw new ApiError(400,"Informe uma URL pública HTTP/HTTPS válida, sem credenciais.");}
  URI u=URI.create(normalized);String host=u.getHost();
  boolean tls="https".equalsIgnoreCase(u.getScheme());int expected=tls?443:80;
  if(u.getPort()!=-1&&u.getPort()!=expected)throw new ApiError(400,"Monitoramento permite apenas portas HTTP/HTTPS padrão.");
  if(host.endsWith(".test")||host.matches("[0-9.]+"))throw new ApiError(400,"Cadastre um domínio público sob seu controle.");
  return (tls?"https://":"http://")+host;
 }
 public static boolean publicIpv4(InetAddress address){
  if(!(address instanceof Inet4Address)||address.isAnyLocalAddress()||address.isLoopbackAddress()||address.isLinkLocalAddress()||address.isSiteLocalAddress()||address.isMulticastAddress())return false;
  byte[] b=address.getAddress();int a=b[0]&255,c=b[1]&255,d=b[2]&255;
  return !(a==0||a==10||a==127||a>=224||a==100&&c>=64&&c<=127||a==169&&c==254||a==172&&c>=16&&c<=31||a==192&&(c==168||c==0||c==88&&d==99)||a==198&&(c==18||c==19||c==51&&d==100)||a==203&&c==0&&d==113);
 }
 InetAddress resolve(String host)throws IOException{
  InetAddress chosen=null;
  for(var address:InetAddress.getAllByName(host)){
   // Public IPv6 is not probed in this version. Any local/reserved answer rejects the origin.
   if(address instanceof Inet6Address){if(address.isLoopbackAddress()||address.isAnyLocalAddress()||address.isLinkLocalAddress()||address.isSiteLocalAddress()||(address.getAddress()[0]&254)==252)throw new IOException("Non-public DNS");continue;}
   if(!publicIpv4(address))throw new IOException("Non-public DNS");
   if(chosen==null)chosen=address;
  }
  if(chosen==null)throw new IOException("No public IPv4");return chosen;
 }
 public enum Ownership { CONFIRMED, MISSING, UNAVAILABLE }
 public boolean owns(String domain,String token){return ownership(domain,token)==Ownership.CONFIRMED;}
 public Ownership ownership(String domain,String token){
  Hashtable<String,String> env=new Hashtable<>();env.put("java.naming.factory.initial","com.sun.jndi.dns.DnsContextFactory");env.put("com.sun.jndi.dns.timeout.initial","2000");env.put("com.sun.jndi.dns.timeout.retries","1");
  try{var context=new InitialDirContext(env);try{var values=context.getAttributes("_sentinela."+domain,new String[]{"TXT"}).get("TXT");if(values==null)return Ownership.MISSING;for(int i=0;i<values.size();i++){String value=values.get(i).toString().replace("\"","").replace(" ","");if(java.security.MessageDigest.isEqual(value.getBytes(StandardCharsets.UTF_8),("sentinela="+token).getBytes(StandardCharsets.UTF_8)))return Ownership.CONFIRMED;}return Ownership.MISSING;}finally{context.close();}}
  catch(javax.naming.NameNotFoundException missing){return Ownership.MISSING;}
  catch(Exception unavailable){return Ownership.UNAVAILABLE;}
 }
 public Result probe(String value){
  long start=System.nanoTime();boolean tls=false;
  try{
   URI origin=URI.create(origin(value));String host=origin.getHost();tls=origin.getScheme().equals("https");InetAddress address=resolve(host);Long expiry=null;
   try(Socket tcp=new Socket()){
    tcp.connect(new InetSocketAddress(address,tls?443:80),3000);tcp.setSoTimeout(4000);
    Socket connection=tcp;
    if(tls){SSLSocket ssl=(SSLSocket)((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(tcp,host,443,true);var params=ssl.getSSLParameters();params.setEndpointIdentificationAlgorithm("HTTPS");params.setServerNames(List.of(new SNIHostName(host)));params.setApplicationProtocols(new String[]{"http/1.1"});ssl.setSSLParameters(params);ssl.setSoTimeout(4000);ssl.startHandshake();expiry=((X509Certificate)ssl.getSession().getPeerCertificates()[0]).getNotAfter().getTime();connection=ssl;}
    try(Socket active=connection){active.getOutputStream().write(("HEAD / HTTP/1.1\r\nHost: "+host+"\r\nUser-Agent: Sentinela-Monitor/1.0\r\nAccept: */*\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));active.getOutputStream().flush();
     var headers=readFinalHeaders(active.getInputStream(),tls);
     String status=headers.code()>=500?"DOWN":headers.code()>=400?"RESTRICTED":headers.code()>=300?"REDIRECT":"UP";
     return new Result(status,(System.nanoTime()-start)/1_000_000,headers.code(),expiry,headers.findings(),"HEAD da raiz; sem seguir redirecionamentos. Não mede ataques nem explora falhas.");
    }
   }
  }catch(Exception failure){String status=failure instanceof SSLException?"TLS_ERROR":"UNREACHABLE";return new Result(status,(System.nanoTime()-start)/1_000_000,0,null,List.of(),"Verificação não concluída. DNS, rede, certificado ou política do destino podem impedir a consulta; isso não comprova ataque.");}
 }
 public static Headers readFinalHeaders(InputStream in,boolean tls)throws IOException {
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();int end=0,total=0,interim=0;long deadline=System.nanoTime()+6_000_000_000L;
  while(total<32768){
   if(System.nanoTime()>deadline)throw new IOException("Read deadline");int value=in.read();if(value<0)throw new IOException("Incomplete headers");bytes.write(value);total++;end=(end<<8)|value;
   if(end==0x0d0a0d0a){String raw=bytes.toString(StandardCharsets.ISO_8859_1);String first=raw.substring(0,raw.indexOf("\r\n"));
    if(!first.matches("HTTP/1\\.[01] [1-5][0-9]{2}( .*)?"))throw new IOException("Invalid status");int code=Integer.parseInt(first.substring(9,12));
    if(code>=200)return parseHeaders(raw,tls);
    if(code==101||++interim>5)throw new IOException("Unexpected protocol upgrade or interim limit");bytes.reset();end=0;
   }
  }
  throw new IOException("Header limit");
 }
 public static Headers parseHeaders(String raw,boolean tls){
  if(raw.length()>32768||!raw.contains("\r\n\r\n"))throw new ApiError(400,"Cabeçalhos inválidos.");String[] lines=raw.substring(0,raw.indexOf("\r\n\r\n")).split("\r\n");
  if(lines.length>128||!lines[0].matches("HTTP/1\\.[01] [1-5][0-9]{2}( .*)?"))throw new ApiError(400,"Resposta HTTP inválida.");
  int code=Integer.parseInt(lines[0].substring(9,12));Map<String,String> headers=new HashMap<>();
  for(int i=1;i<lines.length;i++){int colon=lines[i].indexOf(':');if(colon<=0)throw new ApiError(400,"Cabeçalho inválido.");headers.put(lines[i].substring(0,colon).toLowerCase(Locale.ROOT),lines[i].substring(colon+1).strip());}
  List<Map<String,String>> findings=new ArrayList<>();
  if(!tls)findings.add(Map.of("id","HTTPS","title","Origem usa HTTP","detail","Considere HTTPS. Não houve teste de certificado nessa origem.","severity","MEDIUM"));
  for(var requirement:List.of(new String[]{"content-security-policy","CSP","Content-Security-Policy"},new String[]{"strict-transport-security","HSTS","Strict-Transport-Security"},new String[]{"x-content-type-options","NOSNIFF","X-Content-Type-Options"},new String[]{"referrer-policy","REFERRER","Referrer-Policy"})){
   if(requirement[1].equals("HSTS")&&!tls)continue;
   if(!headers.containsKey(requirement[0]))findings.add(Map.of("id",requirement[1],"title",requirement[2]+" ausente na resposta","detail","Ausência observada em HEAD da raiz. Adequação depende da aplicação; não comprova vulnerabilidade explorável.","severity","LOW"));
  }
  return new Headers(code,List.copyOf(findings));
 }
}
