package br.edu.sentinela;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import jakarta.annotation.PreDestroy;

@Service
public class SiteMonitor {
 final Store s;final PublicSiteProbe probe;final boolean enabled;final TransactionTemplate tx;
 final ThreadPoolExecutor workers=new ThreadPoolExecutor(2,2,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(20),r->{Thread t=new Thread(r,"sentinela-site-monitor");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
 SiteMonitor(Store s,PublicSiteProbe probe,PlatformTransactionManager transactions,@Value("${sentinela.monitoring-enabled:true}")boolean enabled){this.s=s;this.probe=probe;this.enabled=enabled;tx=new TransactionTemplate(transactions);}
 @PreDestroy void stop(){workers.shutdownNow();}
 @Scheduled(fixedDelay=60000,initialDelay=30000)
 public void scheduled(){if(!enabled)return;long now=System.currentTimeMillis();for(var site:s.rows("SELECT m.* FROM monitored_sites m JOIN app_users u ON u.id=m.user_id WHERE m.enabled=TRUE AND m.verified_at IS NOT NULL AND m.next_check<? AND m.lease_until<? AND u.enabled=TRUE AND u.subscription_status IN ('ACTIVE','TRIAL') ORDER BY m.next_check LIMIT 10",now,now))try{enqueue(site);}catch(ApiError ignored){}}
 public void enqueue(Map<String,Object> site){
  if(!enabled)throw new ApiError(503,"Monitoramento está desativado neste ambiente.");long now=System.currentTimeMillis(),lease=now+300000;String id=site.get("id").toString();
  if(s.jdbc().update("UPDATE monitored_sites SET lease_until=?,next_check=? WHERE id=? AND lease_until<? AND revision=? AND enabled=TRUE AND verified_at IS NOT NULL AND user_id IN (SELECT id FROM app_users WHERE enabled=TRUE AND subscription_status IN ('ACTIVE','TRIAL'))",lease,now+300000,id,now,site.get("revision"))!=1)throw new ApiError(409,"Já há uma verificação pendente ou o site/conta foi pausado ou alterado.");
  try{workers.execute(()->check(site,lease));}catch(RejectedExecutionException failure){s.jdbc().update("UPDATE monitored_sites SET lease_until=0,next_check=? WHERE id=? AND lease_until=?",now+60000,id,lease);throw new ApiError(429,"Fila ocupada; tente novamente em instantes.");}
 }
 void check(Map<String,Object> snapshot,long lease){
  if(s.one("SELECT m.id FROM monitored_sites m JOIN app_users u ON u.id=m.user_id WHERE m.id=? AND m.revision=? AND m.lease_until=? AND m.enabled=TRUE AND m.verified_at IS NOT NULL AND u.enabled=TRUE AND u.subscription_status IN ('ACTIVE','TRIAL')",snapshot.get("id"),snapshot.get("revision"),lease)==null)return;
  var ownership=probe.ownership(snapshot.get("domain").toString(),snapshot.get("verification_token").toString());
  boolean owns=ownership==PublicSiteProbe.Ownership.CONFIRMED;
  PublicSiteProbe.Result result=ownership==PublicSiteProbe.Ownership.UNAVAILABLE?new PublicSiteProbe.Result("DNS_UNAVAILABLE",0,0,null,List.of(),"DNS de confirmação indisponível. Nenhum acesso ao destino foi feito; haverá nova tentativa automática."):owns?probe.probe(snapshot.get("origin").toString()):new PublicSiteProbe.Result("VERIFICATION_LOST",0,0,null,List.of(),"Confirmação DNS não encontrada. Verifique o domínio novamente.");
  tx.executeWithoutResult(status->{
   var owner=s.one("SELECT enabled,subscription_status FROM app_users WHERE id=? FOR UPDATE",snapshot.get("user_id"));
   var current=s.one("SELECT * FROM monitored_sites WHERE id=? FOR UPDATE",snapshot.get("id"));
   if(owner==null||!Boolean.TRUE.equals(owner.get("enabled"))||!List.of("ACTIVE","TRIAL").contains(owner.get("subscription_status"))||current==null||!Boolean.TRUE.equals(current.get("enabled"))||current.get("verified_at")==null||!Objects.equals(current.get("revision"),snapshot.get("revision"))||((Number)current.get("lease_until")).longValue()!=lease)return;
   long now=System.currentTimeMillis();s.jdbc().update("INSERT INTO site_checks VALUES(?,?,?,?,?,?,?,?,?)",UUID.randomUUID().toString(),current.get("id"),current.get("revision"),now,result.status(),result.responseMs(),result.httpCode(),result.tlsExpires(),s.encode(result));
   s.jdbc().update("UPDATE monitored_sites SET lease_until=0,next_check=?,verified_at=? WHERE id=?",now+300000,ownership==PublicSiteProbe.Ownership.MISSING?null:current.get("verified_at"),current.get("id"));
  });
 }
}
