package br.edu.sentinela;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1")
public class SiteController {
 final Store s;final Crypto c;final PublicSiteProbe probe;final SiteMonitor monitor;final AuthController auth;
 SiteController(Store s,Crypto c,PublicSiteProbe probe,SiteMonitor monitor,AuthController auth){this.s=s;this.c=c;this.probe=probe;this.monitor=monitor;this.auth=auth;}
 public record Input(@NotBlank @Size(max=80)String name,@NotBlank @Size(max=2048)String url){}
 public record Toggle(@NotNull Boolean enabled){}
 Map<String,Object> owned(SessionFilter.Viewer viewer,String id){var row=s.one("SELECT * FROM monitored_sites WHERE id=? AND user_id=?",id,viewer.id());if(row==null)throw new ApiError(404,"Site não encontrado.");return row;}
 Map<String,Object> view(Map<String,Object> row){Map<String,Object> v=new LinkedHashMap<>();for(String key:List.of("id","name","origin","domain","revision","enabled","created_at","verified_at","next_check"))v.put(key,row.get(key));v.put("verified",row.get("verified_at")!=null);v.put("integrationConfigured",row.get("integration_hash")!=null);v.put("checking",((Number)row.get("lease_until")).longValue()>System.currentTimeMillis());v.put("dnsName","_sentinela."+row.get("domain"));v.put("dnsValue","sentinela="+row.get("verification_token"));return v;}
 @GetMapping("/sites") Object list(@AuthenticationPrincipal SessionFilter.Viewer v){return s.rows("SELECT * FROM monitored_sites WHERE user_id=? ORDER BY created_at DESC",v.id()).stream().map(this::view).toList();}
 @PostMapping("/sites") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @Transactional
 public Object create(@AuthenticationPrincipal SessionFilter.Viewer v,@Valid @RequestBody Input b){
  s.limit("site-create:"+v.id(),10,3600);auth.lockViewer(v);
  if(((Number)s.one("SELECT COUNT(*) AS n FROM monitored_sites WHERE user_id=?",v.id()).get("n")).intValue()>=5)throw new ApiError(409,"Limite de cinco sites por conta.");
  String origin=PublicSiteProbe.origin(b.url()),id=UUID.randomUUID().toString();String name=b.name().strip();if(name.isBlank()||name.chars().anyMatch(x->x<32))throw new ApiError(400,"Nome inválido.");
  if(s.one("SELECT id FROM monitored_sites WHERE user_id=? AND origin=?",v.id(),origin)!=null)throw new ApiError(409,"Esta origem já está cadastrada na sua conta.");
  s.jdbc().update("INSERT INTO monitored_sites(id,user_id,name,origin,domain,verification_token,created_at) VALUES(?,?,?,?,?,?,?)",id,v.id(),name,origin,URI.create(origin).getHost(),c.token(),System.currentTimeMillis());s.audit(v.id(),"SITE_CREATED",id);return view(owned(v,id));
 }
 @PatchMapping("/sites/{id}") @Transactional
 public Object edit(@AuthenticationPrincipal SessionFilter.Viewer v,@PathVariable String id,@Valid @RequestBody Input b){
  auth.lockViewer(v);var previous=owned(v,id);String origin=PublicSiteProbe.origin(b.url()),name=b.name().strip();if(name.isBlank()||name.chars().anyMatch(x->x<32))throw new ApiError(400,"Nome inválido.");
  if(s.one("SELECT id FROM monitored_sites WHERE user_id=? AND origin=? AND id<>?",v.id(),origin,id)!=null)throw new ApiError(409,"Esta origem já está cadastrada.");
  if(!origin.equals(previous.get("origin")))s.jdbc().update("UPDATE monitored_sites SET name=?,origin=?,domain=?,revision=revision+1,verification_token=?,verified_at=NULL,integration_hash=NULL,lease_until=0,next_check=0 WHERE id=?",name,origin,URI.create(origin).getHost(),c.token(),id);
  else s.jdbc().update("UPDATE monitored_sites SET name=? WHERE id=?",name,id);
  s.audit(v.id(),"SITE_UPDATED",id);return view(owned(v,id));
 }
 @PatchMapping("/sites/{id}/status") @Transactional
 public Object toggle(@AuthenticationPrincipal SessionFilter.Viewer v,@PathVariable String id,@Valid @RequestBody Toggle b){auth.lockViewer(v);owned(v,id);s.jdbc().update("UPDATE monitored_sites SET enabled=?,lease_until=0 WHERE id=?",b.enabled(),id);s.audit(v.id(),"SITE_STATUS",id+" ativo="+b.enabled());return view(owned(v,id));}
 @PostMapping("/sites/{id}/verify")
 public Object verify(@AuthenticationPrincipal SessionFilter.Viewer v,@PathVariable String id){s.limit("site-verify:"+v.id(),10,900);var row=owned(v,id);var ownership=probe.ownership(row.get("domain").toString(),row.get("verification_token").toString());if(ownership==PublicSiteProbe.Ownership.UNAVAILABLE)throw new ApiError(503,"Não foi possível consultar o DNS agora; tente novamente em instantes.");if(ownership!=PublicSiteProbe.Ownership.CONFIRMED)throw new ApiError(409,"Registro TXT ainda não encontrado. Confira nome, valor e propagação DNS.");
  int changed=s.jdbc().update("UPDATE monitored_sites SET verified_at=?,next_check=0 WHERE id=? AND user_id=? AND revision=? AND verification_token=?",System.currentTimeMillis(),id,v.id(),row.get("revision"),row.get("verification_token"));if(changed!=1)throw new ApiError(409,"Site alterado; repita a confirmação.");s.audit(v.id(),"SITE_VERIFIED",id);return view(owned(v,id));}
 @PostMapping("/sites/{id}/check") Object check(@AuthenticationPrincipal SessionFilter.Viewer v,@PathVariable String id){s.limit("site-check:"+id,1,60);var row=owned(v,id);if(row.get("verified_at")==null||!Boolean.TRUE.equals(row.get("enabled")))throw new ApiError(409,"Confirme o domínio e ative o site antes de monitorar.");monitor.enqueue(row);return Map.of("queued",true);}
 @GetMapping("/sites/{id}/dashboard") Object dashboard(@AuthenticationPrincipal SessionFilter.Viewer v,@PathVariable String id,@RequestParam(defaultValue="7")int days){
  if(days!=1&&days!=7&&days!=30)throw new ApiError(400,"Período inválido.");var row=owned(v,id);long cutoff=System.currentTimeMillis()-days*86400000L;
  var checks=new ArrayList<>(s.rows("SELECT checked_at,status,response_ms,http_code,tls_expires FROM site_checks WHERE site_id=? AND revision=? AND checked_at>=? ORDER BY checked_at DESC LIMIT 2000",id,row.get("revision"),cutoff));Collections.reverse(checks);
  var latest=s.one("SELECT result_json FROM site_checks WHERE site_id=? AND revision=? ORDER BY checked_at DESC LIMIT 1",id,row.get("revision"));
  var counts=s.one("SELECT COUNT(*) AS total,SUM(CASE WHEN status IN ('UP','REDIRECT') THEN 1 ELSE 0 END) AS successful FROM site_checks WHERE site_id=? AND revision=? AND checked_at>=? AND status NOT IN ('VERIFICATION_LOST','DNS_UNAVAILABLE')",id,row.get("revision"),cutoff);
  long total=((Number)counts.get("total")).longValue();long good=counts.get("successful")==null?0:((Number)counts.get("successful")).longValue();
  var events=s.rows("SELECT event_id,type,simulated,occurred_at,received_at FROM site_events WHERE site_id=? AND revision=? AND received_at>=? ORDER BY received_at DESC LIMIT 100",id,row.get("revision"),cutoff);
  var eventCounts=s.one("SELECT COUNT(*) AS total,SUM(CASE WHEN simulated=TRUE THEN 1 ELSE 0 END) AS simulated FROM site_events WHERE site_id=? AND revision=? AND received_at>=?",id,row.get("revision"),cutoff);long eventTotal=((Number)eventCounts.get("total")).longValue(),simulated=eventCounts.get("simulated")==null?0:((Number)eventCounts.get("simulated")).longValue();
  var series=s.rows("SELECT CAST(FLOOR(received_at / 3600000.0) AS BIGINT) AS bucket,COUNT(*) AS total FROM site_events WHERE site_id=? AND revision=? AND received_at>=? AND simulated=FALSE GROUP BY CAST(FLOOR(received_at / 3600000.0) AS BIGINT) ORDER BY bucket",id,row.get("revision"),cutoff);
  Map<String,Object> result=new LinkedHashMap<>();result.put("site",view(row));result.put("availability",total==0?null:Math.round(10000.0*good/total)/100.0);result.put("sampleCount",total);result.put("latest",latest==null?null:s.decode(latest.get("result_json").toString()));result.put("checks",checks);result.put("events",events);result.put("eventSeries",series);result.put("reportedEvents",eventTotal-simulated);result.put("simulatedEvents",simulated);result.put("periodDays",days);result.put("notice","Disponibilidade nas amostras, não medição contínua. Eventos são relatos do conector; não comprovam invasão. Gráficos limitados às 2.000 últimas amostras; contadores usam o período completo.");return result;
 }
 @PostMapping("/sites/{id}/integration-key") @Transactional
 public Object key(@AuthenticationPrincipal SessionFilter.Viewer v,@PathVariable String id,@Valid @RequestBody AuthController.Proof b){
  s.limit("site-key:"+v.id(),5,900);var user=auth.lockViewer(v);auth.password(v.id(),b.password());if(Boolean.TRUE.equals(user.get("mfa_enabled")))auth.verifyMfaProof(user,b.code());var row=owned(v,id);if(row.get("verified_at")==null)throw new ApiError(409,"Confirme o domínio antes de integrar.");String key=c.token();s.jdbc().update("UPDATE monitored_sites SET integration_hash=? WHERE id=?",Crypto.hash(key),id);s.audit(v.id(),"SITE_KEY_ROTATED",id);return Map.of("key",key,"message","Exibida uma vez; use apenas no backend do seu site. A chave anterior foi revogada.");
 }
 public record Event(@NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}")String eventId,@NotBlank @Pattern(regexp="AUTH_FAILURE|RATE_LIMIT|WAF_BLOCK|INTEGRITY_ALERT|APPLICATION_ERROR")String type,@NotNull Long occurredAt,@NotNull Boolean simulated){}
 @PostMapping("/ingest/events") @Transactional
 public Object ingest(HttpServletRequest req,@Valid @RequestBody Event event){
  if(req.getHeader("Authorization")!=null||req.getCookies()!=null)throw new ApiError(400,"Não misture sessão humana com chave do site.");String key=req.getHeader("X-Sentinela-Site-Key");if(key==null||key.length()>200||key.length()<32)throw new ApiError(401,"Integração não autorizada.");
  var initial=s.one("SELECT * FROM monitored_sites WHERE integration_hash=?",Crypto.hash(key));if(initial==null)throw new ApiError(401,"Integração não autorizada.");
  var owner=s.one("SELECT enabled,subscription_status FROM app_users WHERE id=? FOR UPDATE",initial.get("user_id"));var site=s.one("SELECT * FROM monitored_sites WHERE id=? FOR UPDATE",initial.get("id"));
  if(site==null||!Objects.equals(site.get("integration_hash"),Crypto.hash(key)))throw new ApiError(401,"Chave revogada.");
  if(owner==null||!Boolean.TRUE.equals(owner.get("enabled"))||!List.of("ACTIVE","TRIAL").contains(owner.get("subscription_status"))||site.get("verified_at")==null||!Boolean.TRUE.equals(site.get("enabled")))throw new ApiError(403,"Integração pausada ou domínio não confirmado.");
  s.limit("ingest-global",300,60);s.limit("ingest:"+site.get("id"),60,60);long now=System.currentTimeMillis();if(event.occurredAt()<now-300000||event.occurredAt()>now+60000)throw new ApiError(400,"Evento fora da janela de cinco minutos.");
  int created=s.jdbc().update("INSERT INTO site_events VALUES(?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",UUID.randomUUID().toString(),site.get("id"),site.get("revision"),event.eventId(),event.type(),event.simulated(),event.occurredAt(),now);return Map.of("accepted",created==1);
 }
 @GetMapping("/admin/sites") Object allSites(){return s.rows("SELECT m.id,m.name,m.origin,m.domain,m.enabled,m.verified_at,m.created_at,u.name AS owner_name,u.email AS owner_email FROM monitored_sites m JOIN app_users u ON u.id=m.user_id ORDER BY m.created_at DESC LIMIT 200");}
 public record Question(@NotBlank @Size(max=1500)String question){}
 @PostMapping("/sites/{id}/assistant") Object assistant(@AuthenticationPrincipal SessionFilter.Viewer v,@PathVariable String id,@Valid @RequestBody Question b){
  s.limit("site-assistant:"+v.id(),15,60);var data=(Map<String,Object>)dashboard(v,id,7);var site=(Map<String,Object>)data.get("site");var latest=(Map<String,Object>)data.get("latest");StringBuilder answer=new StringBuilder("Site: "+site.get("origin")+". ");
  if(!Boolean.TRUE.equals(site.get("verified")))answer.append("O domínio ainda precisa de confirmação DNS. Não há monitoramento externo ativo. ");
  if(latest==null)answer.append("Ainda não há amostras para avaliar disponibilidade, resposta ou certificado. ");
  else{answer.append("Última verificação externa: ").append(latest.get("status")).append("; HTTP ").append(latest.get("httpCode")).append("; tempo ").append(latest.get("responseMs")).append(" ms. ");answer.append(latest.get("message")).append(" ");var findings=(List<Map<String,Object>>)latest.get("findings");if(findings!=null)for(var finding:findings)answer.append(finding.get("title")).append(". ");}
  answer.append("Eventos reportados pelo conector nos últimos sete dias: ").append(data.get("reportedEvents")).append("; eventos simulados: ").append(data.get("simulatedEvents")).append(". Esses relatos não comprovam invasão. Ausência de eventos também não comprova ausência de ataques. ");
  if(b.question().toLowerCase(Locale.ROOT).matches("(?s).*(domínio|dominio|alterar|trocar).*"))answer.append("Use Editar site para trocar a origem. Isso exige nova confirmação DNS e revoga a chave do conector anterior. ");
  answer.append("Nenhuma verificação garante segurança 100%. Este resumo usa somente medições registradas; não executa ações no seu site.");return Map.of("answer",answer.toString(),"mode","MONITOR_EVIDENCE");
 }
 @GetMapping("/admin/subscriptions") Object subscriptions(){return s.rows("SELECT id,name,email,enabled,subscription_status,subscription_due FROM app_users WHERE role='USER' ORDER BY created_at DESC LIMIT 200");}
 public record Billing(@NotBlank @Pattern(regexp="ACTIVE|TRIAL|PAST_DUE|SUSPENDED")String status,Long dueAt){}
 @PatchMapping("/admin/subscriptions/{id}") @Transactional
 public Object billing(@AuthenticationPrincipal SessionFilter.Viewer v,@PathVariable String id,@Valid @RequestBody Billing b){var row=s.one("SELECT * FROM app_users WHERE id=? AND role='USER' FOR UPDATE",id);if(row==null)throw new ApiError(404,"Cliente não encontrado.");boolean active=List.of("ACTIVE","TRIAL").contains(b.status());if(b.dueAt()!=null&&b.dueAt()<0)throw new ApiError(400,"Vencimento inválido.");s.jdbc().update("UPDATE app_users SET subscription_status=?,subscription_due=?,enabled=? WHERE id=?",b.status(),b.dueAt(),active,id);if(!active)s.jdbc().update("DELETE FROM sessions WHERE user_id=?",id);s.audit(v.id(),"SUBSCRIPTION_UPDATED",id+" situação="+b.status());return Map.of("ok",true);}
}
