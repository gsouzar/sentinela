package br.edu.sentinela;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.*;import java.util.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.web.servlet.MockMvc;
@SpringBootTest(properties={"spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:sites;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}","sentinela.encryption-key=test-only-encryption-key-not-for-production","sentinela.require-mfa=false","sentinela.monitoring-enabled=false"}) @AutoConfigureMockMvc
@org.springframework.test.context.TestExecutionListeners(listeners={org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,org.springframework.test.context.web.ServletTestExecutionListener.class},mergeMode=org.springframework.test.context.TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
class SiteFlowTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired Store s;
 @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
 String body(Object...args)throws Exception{Map<String,Object> m=new HashMap<>();for(int i=0;i<args.length;i+=2)m.put(args[i].toString(),args[i+1]);return json.writeValueAsString(m);}
 @BeforeEach void limits(){s.jdbc().update("DELETE FROM rate_buckets");}
 String account()throws Exception{return json.readTree(mvc.perform(post("/api/v1/auth/register").header("X-Sentinela-Client","android").contentType("application/json").content(body("name","Cliente","email",UUID.randomUUID()+"@example.org","password","UmaSenhaForte#123"))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("accessToken").asText();}
 JsonNode site(String token)throws Exception{return json.readTree(mvc.perform(post("/api/v1/sites").header("Authorization","Bearer "+token).contentType("application/json").content(body("name","Meu site","url","https://example.org/path?private=secret"))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());}
 @Test void separatesSitesAndShowsNoInventedAvailability()throws Exception{
  String a=account(),b=account();var site=site(a);String id=site.path("id").asText();assertEquals("https://example.org",site.path("origin").asText());assertFalse(site.path("verified").asBoolean());
  mvc.perform(get("/api/v1/sites/"+id+"/dashboard").header("Authorization","Bearer "+b)).andExpect(status().isNotFound());
  var dashboard=json.readTree(mvc.perform(get("/api/v1/sites/"+id+"/dashboard").header("Authorization","Bearer "+a)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  assertTrue(dashboard.path("availability").isNull());assertEquals(0,dashboard.path("checks").size());
  mvc.perform(post("/api/v1/sites/"+id+"/check").header("Authorization","Bearer "+a)).andExpect(status().isConflict());
 }
 @Test void changingDomainRevokesProofAndKeyAndSeparatesHistory()throws Exception{
  String token=account();String id=site(token).path("id").asText();s.jdbc().update("UPDATE monitored_sites SET verified_at=?,integration_hash=? WHERE id=?",System.currentTimeMillis(),Crypto.hash("old-key"),id);
  s.jdbc().update("INSERT INTO site_checks VALUES(?,?,?,?,?,?,?,?,?)",UUID.randomUUID().toString(),id,1,System.currentTimeMillis(),"UP",45,200,null,"{}");
  var changed=json.readTree(mvc.perform(patch("/api/v1/sites/"+id).header("Authorization","Bearer "+token).contentType("application/json").content(body("name","Novo domínio","url","https://example.net"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());assertEquals(2,changed.path("revision").asInt());assertFalse(changed.path("verified").asBoolean());assertFalse(changed.path("integrationConfigured").asBoolean());
  var dash=json.readTree(mvc.perform(get("/api/v1/sites/"+id+"/dashboard").header("Authorization","Bearer "+token)).andReturn().getResponse().getContentAsString());assertEquals(0,dash.path("checks").size());
 }
 @Test void scopedIngestIsIdempotentAndSimulationIsDistinct()throws Exception{
  String token=account();String id=site(token).path("id").asText();s.jdbc().update("UPDATE monitored_sites SET verified_at=? WHERE id=?",System.currentTimeMillis(),id);
  mvc.perform(post("/api/v1/ingest/events").contentType("application/json").content(body("eventId",UUID.randomUUID().toString(),"type","AUTH_FAILURE","occurredAt",System.currentTimeMillis(),"simulated",true))).andExpect(status().isUnauthorized());
  var key=json.readTree(mvc.perform(post("/api/v1/sites/"+id+"/integration-key").header("Authorization","Bearer "+token).contentType("application/json").content(body("password","UmaSenhaForte#123"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("key").asText();assertTrue(key.length()>=32);
  String event=body("eventId",UUID.randomUUID().toString(),"type","WAF_BLOCK","occurredAt",System.currentTimeMillis(),"simulated",true);
  for(int i=0;i<2;i++)mvc.perform(post("/api/v1/ingest/events").header("X-Sentinela-Site-Key",key).contentType("application/json").content(event)).andExpect(status().isOk());
  var dash=json.readTree(mvc.perform(get("/api/v1/sites/"+id+"/dashboard").header("Authorization","Bearer "+token)).andReturn().getResponse().getContentAsString());assertEquals(1,dash.path("simulatedEvents").asInt());assertEquals(0,dash.path("reportedEvents").asInt());
  mvc.perform(patch("/api/v1/sites/"+id+"/status").header("Authorization","Bearer "+token).contentType("application/json").content(body("enabled",false))).andExpect(status().isOk());
  mvc.perform(post("/api/v1/ingest/events").header("X-Sentinela-Site-Key",key).contentType("application/json").content(event)).andExpect(status().isForbidden());
 }
 @Test void billingAndSiteAdministrationAreNotClientPermissions()throws Exception{
  String token=account();mvc.perform(get("/api/v1/admin/sites").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
  mvc.perform(patch("/api/v1/admin/subscriptions/unknown").header("Authorization","Bearer "+token).contentType("application/json").content(body("status","PAST_DUE"))).andExpect(status().isForbidden());
 }
 @Test void availabilityUsesPersistedSamplesAndAssistantDoesNotClaimAnAttack()throws Exception{
  String token=account(),id=site(token).path("id").asText();long now=System.currentTimeMillis();
  for(int i=0;i<2;i++)s.jdbc().update("INSERT INTO site_checks VALUES(?,?,?,?,?,?,?,?,?)",UUID.randomUUID().toString(),id,1,now+i,i==0?"UP":"DOWN",25, i==0?200:503,null,body("status",i==0?"UP":"DOWN","responseMs",25,"httpCode",i==0?200:503,"message","Medição de teste","findings",List.of()));
  long previousHour=now/3600000-1;long eventTime=previousHour*3600000+2700000;s.jdbc().update("INSERT INTO site_events VALUES(?,?,?,?,?,?,?,?)",UUID.randomUUID().toString(),id,1,UUID.randomUUID().toString(),"AUTH_FAILURE",false,eventTime,eventTime);
  var dash=json.readTree(mvc.perform(get("/api/v1/sites/"+id+"/dashboard").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());assertEquals(50.0,dash.path("availability").asDouble());assertEquals(2,dash.path("checks").size());
  assertEquals(previousHour,dash.path("eventSeries").get(0).path("bucket").asLong());
  var response=mvc.perform(post("/api/v1/sites/"+id+"/assistant").header("Authorization","Bearer "+token).contentType("application/json").content(body("question","Meu site foi atacado?"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertTrue(response.contains("não comprovam invasão"));
 }
 @Test void staleQueuedWorkDoesNotProbeAChangedDomain()throws Exception{
  String token=account(),id=site(token).path("id").asText();s.jdbc().update("UPDATE monitored_sites SET verified_at=?,lease_until=42 WHERE id=?",System.currentTimeMillis(),id);var snapshot=s.one("SELECT * FROM monitored_sites WHERE id=?",id);
  var calls=new java.util.concurrent.atomic.AtomicInteger();var fake=new PublicSiteProbe(){@Override public boolean owns(String d,String t){calls.incrementAndGet();return true;}@Override public Result probe(String o){calls.incrementAndGet();return new Result("UP",1,200,null,List.of(),"test");}};
  s.jdbc().update("UPDATE monitored_sites SET revision=2,verified_at=NULL,lease_until=0 WHERE id=?",id);var monitor=new SiteMonitor(s,fake,transactions,true);try{monitor.check(snapshot,42);assertEquals(0,calls.get());}finally{monitor.stop();}
 }
 @Test void rotatesAndRevokesScopedIntegrationKeys()throws Exception{
  String token=account(),id=site(token).path("id").asText();s.jdbc().update("UPDATE monitored_sites SET verified_at=? WHERE id=?",System.currentTimeMillis(),id);String old="";
  for(int i=0;i<2;i++){String key=json.readTree(mvc.perform(post("/api/v1/sites/"+id+"/integration-key").header("Authorization","Bearer "+token).contentType("application/json").content(body("password","UmaSenhaForte#123"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("key").asText();if(i==0)old=key;else assertNotEquals(old,key);}
  mvc.perform(post("/api/v1/ingest/events").header("X-Sentinela-Site-Key",old).contentType("application/json").content(body("eventId",UUID.randomUUID().toString(),"type","AUTH_FAILURE","occurredAt",System.currentTimeMillis(),"simulated",false))).andExpect(status().isUnauthorized());
 }
 @Test void temporaryDnsFailureDoesNotRevokeAnExistingDomainProof()throws Exception{
  String token=account(),id=site(token).path("id").asText();s.jdbc().update("UPDATE monitored_sites SET verified_at=?,lease_until=42 WHERE id=?",System.currentTimeMillis(),id);var snapshot=s.one("SELECT * FROM monitored_sites WHERE id=?",id);
  var fake=new PublicSiteProbe(){@Override public Ownership ownership(String d,String t){return Ownership.UNAVAILABLE;}@Override public Result probe(String origin){throw new AssertionError("Must not access origin without current ownership confirmation");}};
  var monitor=new SiteMonitor(s,fake,transactions,true);try{monitor.check(snapshot,42);assertNotNull(s.one("SELECT verified_at FROM monitored_sites WHERE id=?",id).get("verified_at"));assertEquals("DNS_UNAVAILABLE",s.one("SELECT status FROM site_checks WHERE site_id=?",id).get("status"));}finally{monitor.stop();}
 }
 @Test void adminSuspensionRevokesAccessAndPreservesClientSites()throws Exception{
  String customer=account(),admin=account();String siteId=site(customer).path("id").asText();var customerId=s.one("SELECT user_id FROM monitored_sites WHERE id=?",siteId).get("user_id");var adminId=s.one("SELECT user_id FROM sessions WHERE token_hash=?",Crypto.hash(admin)).get("user_id");
  s.jdbc().update("UPDATE app_users SET role='ADMIN',mfa_enabled=TRUE WHERE id=?",adminId);
  mvc.perform(patch("/api/v1/admin/subscriptions/"+customerId).header("Authorization","Bearer "+admin).contentType("application/json").content(body("status","PAST_DUE","dueAt",System.currentTimeMillis()))).andExpect(status().isOk());
  mvc.perform(get("/api/v1/me").header("Authorization","Bearer "+customer)).andExpect(status().isUnauthorized());assertNotNull(s.one("SELECT id FROM monitored_sites WHERE id=?",siteId));assertEquals("PAST_DUE",s.one("SELECT subscription_status FROM app_users WHERE id=?",customerId).get("subscription_status"));
  mvc.perform(patch("/api/v1/admin/subscriptions/"+customerId).header("Authorization","Bearer "+admin).contentType("application/json").content(body("status","ACTIVE"))).andExpect(status().isOk());assertEquals(true,s.one("SELECT enabled FROM app_users WHERE id=?",customerId).get("enabled"));
  mvc.perform(get("/api/v1/admin/sites").header("Authorization","Bearer "+admin)).andExpect(status().isOk());
 }
}
