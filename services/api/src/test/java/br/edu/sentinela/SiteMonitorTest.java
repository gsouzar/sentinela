package br.edu.sentinela;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
class SiteMonitorTest {
 @Test void originIsCanonicalAndRestrictedToDefaultPorts(){
  assertEquals("https://example.org",PublicSiteProbe.origin("https://example.org/path?q=secret#fragment"));
  assertThrows(ApiError.class,()->PublicSiteProbe.origin("https://example.org:8443"));
  assertThrows(ApiError.class,()->PublicSiteProbe.origin("http://127.0.0.1"));
  assertThrows(ApiError.class,()->PublicSiteProbe.origin("https://user:pass@example.org"));
 }
 @Test void excludesPrivateReservedAndMappedNetworks()throws Exception{
  for(String ip:new String[]{"127.0.0.1","10.1.2.3","192.168.1.1","169.254.169.254","100.64.0.1","192.0.2.1","198.18.0.1","203.0.113.1","224.1.1.1","::1","::ffff:127.0.0.1"}) assertFalse(PublicSiteProbe.publicIpv4(InetAddress.getByName(ip)),ip);
  assertTrue(PublicSiteProbe.publicIpv4(InetAddress.getByName("93.184.216.34")));
 }
 @Test void parserDoesNotNeedBodyAndDoesNotFollowRedirects()throws Exception{
  var result=PublicSiteProbe.parseHeaders("HTTP/1.1 302 Found\r\nLocation: http://127.0.0.1/private\r\nContent-Security-Policy: default-src 'self'\r\n\r\n",true);
  assertEquals(302,result.code());assertFalse(result.findings().toString().contains("127.0.0.1"));
  assertTrue(result.findings().stream().anyMatch(x->x.get("id").equals("HSTS")));
  assertThrows(ApiError.class,()->PublicSiteProbe.parseHeaders("invalid\r\n\r\n",true));
 }
 @Test void informationalResponsesCannotHideAFinalServerFailure()throws Exception{
  String input="HTTP/1.1 103 Early Hints\r\nLink: </style.css>; rel=preload\r\n\r\nHTTP/1.1 500 Server Error\r\nContent-Security-Policy: default-src 'none'\r\n\r\n";
  assertEquals(500,PublicSiteProbe.readFinalHeaders(new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)),true).code());
  assertThrows(java.io.IOException.class,()->PublicSiteProbe.readFinalHeaders(new java.io.ByteArrayInputStream("HTTP/1.1 101 Switching Protocols\r\n\r\n".getBytes()),true));
 }
}
