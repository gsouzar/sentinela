package br.edu.sentinela;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.*;import java.util.*;import java.util.concurrent.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.web.servlet.MockMvc;import org.springframework.transaction.support.TransactionTemplate;import org.springframework.transaction.PlatformTransactionManager;
@SpringBootTest(properties={"spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:hardening;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}","sentinela.encryption-key=test-only-encryption-key-not-for-production","sentinela.require-mfa=true","sentinela.demo=true","sentinela.telegram-api-key=test-only-telegram-integration-key-32-chars"}) @AutoConfigureMockMvc
@org.springframework.test.context.TestExecutionListeners(listeners={org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,org.springframework.test.context.web.ServletTestExecutionListener.class},mergeMode=org.springframework.test.context.TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
class SecurityHardeningTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired Store s;@Autowired Crypto crypto;@Autowired PlatformTransactionManager transactions;
 @BeforeEach void limits(){s.jdbc().update("DELETE FROM rate_buckets");}
 String body(Object...v)throws Exception{Map<String,Object>b=new HashMap<>();for(int i=0;i<v.length;i+=2)b.put(v[i].toString(),v[i+1]);return json.writeValueAsString(b);}
 JsonNode register(String email)throws Exception{return json.readTree(mvc.perform(post("/api/v1/auth/register").header("X-Sentinela-Client","android").contentType("application/json").content(body("name","Teste Segurança","email",email,"password","UmaSenhaForte#123"))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());}
 String email(){return UUID.randomUUID()+"@example.org";}
 @Test void failedLoginRemainsAuditableAfterTransactionRollback()throws Exception{
 long before=((Number)s.one("SELECT COUNT(*) AS n FROM audit_events WHERE action='LOGIN_FAILED'").get("n")).longValue();
 mvc.perform(post("/api/v1/auth/login").header("X-Sentinela-Client","android").contentType("application/json").content(body("email",email(),"password","wrong-password"))).andExpect(status().isUnauthorized());
 assertEquals(before+1,((Number)s.one("SELECT COUNT(*) AS n FROM audit_events WHERE action='LOGIN_FAILED'").get("n")).longValue());
 }
 @Test void accountMustEnrollBeforeAnalyzingAndCannotDisableRequiredFactor()throws Exception{
 var user=register(email());String token=user.path("accessToken").asText();assertTrue(user.path("user").path("mfa_required").asBoolean());
 mvc.perform(post("/api/v1/analyses").header("Authorization","Bearer "+token).contentType("application/json").content(body("url","https://example.org"))).andExpect(status().isPreconditionRequired());
 var setup=json.readTree(mvc.perform(post("/api/v1/me/mfa/setup").header("Authorization","Bearer "+token).contentType("application/json").content(body("password","UmaSenhaForte#123"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
 String code=crypto.totp(setup.path("secret").asText(),System.currentTimeMillis()/30000);
 mvc.perform(post("/api/v1/me/mfa/enable").header("Authorization","Bearer "+token).contentType("application/json").content(body("password","UmaSenhaForte#123","code",code))).andExpect(status().isOk());
 mvc.perform(post("/api/v1/analyses").header("Authorization","Bearer "+token).contentType("application/json").content(body("url","https://example.org"))).andExpect(status().isCreated());
 mvc.perform(post("/api/v1/me/mfa/disable").header("Authorization","Bearer "+token).contentType("application/json").content(body("password","UmaSenhaForte#123","code",code))).andExpect(status().isForbidden());
 }
 @Test void androidHeaderCannotBypassCsrfWhenBrowserCookieIsPresent()throws Exception{
 mvc.perform(post("/api/v1/auth/register").cookie(new jakarta.servlet.http.Cookie("SENTINELA_SESSION","browser-session")).header("X-Sentinela-Client","android").contentType("application/json").content(body("name","Teste","email",email(),"password","UmaSenhaForte#123"))).andExpect(status().isForbidden());
 }
 @Test void loginRechecksMfaAfterWaitingForAccountLock()throws Exception{
 String email=email();var user=register(email);String id=user.path("user").path("id").asText();ExecutorService pool=Executors.newSingleThreadExecutor();java.util.concurrent.atomic.AtomicReference<Future<Integer>> result=new java.util.concurrent.atomic.AtomicReference<>();
 try{new TransactionTemplate(transactions).executeWithoutResult(tx->{s.one("SELECT * FROM app_users WHERE id=? FOR UPDATE",id);result.set(pool.submit(()->mvc.perform(post("/api/v1/auth/login").header("X-Sentinela-Client","android").contentType("application/json").content(body("email",email,"password","UmaSenhaForte#123"))).andReturn().getResponse().getStatus()));try{Thread.sleep(350);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}s.jdbc().update("UPDATE app_users SET mfa_enabled=TRUE,mfa_secret=? WHERE id=?",crypto.encrypt(crypto.newTotpSecret()),id);s.jdbc().update("DELETE FROM sessions WHERE user_id=?",id);});assertEquals(428,result.get().get(10,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
 }
 @Test void pendingSetupCannotReplaceAnActivatedFactor()throws Exception{
 var user=register(email());String id=user.path("user").path("id").asText(),token=user.path("accessToken").asText(),secret=crypto.newTotpSecret();ExecutorService pool=Executors.newSingleThreadExecutor();java.util.concurrent.atomic.AtomicReference<Future<Integer>> result=new java.util.concurrent.atomic.AtomicReference<>();
 try{new TransactionTemplate(transactions).executeWithoutResult(tx->{s.one("SELECT * FROM app_users WHERE id=? FOR UPDATE",id);result.set(pool.submit(()->mvc.perform(post("/api/v1/me/mfa/setup").header("Authorization","Bearer "+token).contentType("application/json").content(body("password","UmaSenhaForte#123"))).andReturn().getResponse().getStatus()));try{Thread.sleep(350);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}s.jdbc().update("UPDATE app_users SET mfa_enabled=TRUE,mfa_secret=? WHERE id=?",crypto.encrypt(secret),id);});assertEquals(409,result.get().get(10,TimeUnit.SECONDS));assertEquals(secret,crypto.decrypt(s.one("SELECT mfa_secret FROM app_users WHERE id=?",id).get("mfa_secret").toString()));}finally{pool.shutdownNow();}
 }
 @Test void concurrentRefreshReuseRevokesTheWinningSession()throws Exception{
 var u=register(email());String refresh=u.path("refreshToken").asText();ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier barrier=new CyclicBarrier(2);
 try{Callable<org.springframework.mock.web.MockHttpServletResponse> request=()->{barrier.await();return mvc.perform(post("/api/v1/auth/refresh").header("X-Sentinela-Client","android").contentType("application/json").content(body("refreshToken",refresh))).andReturn().getResponse();};var a=pool.submit(request);var b=pool.submit(request);var first=a.get(15,TimeUnit.SECONDS);var second=b.get(15,TimeUnit.SECONDS);assertEquals(Set.of(200,401),Set.of(first.getStatus(),second.getStatus()));var winner=first.getStatus()==200?first:second;String token=json.readTree(winner.getContentAsString()).path("accessToken").asText();mvc.perform(get("/api/v1/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());}finally{pool.shutdownNow();}
 }

 @Test void telegramRequiresKeyAndKeepsConversationOwnership() throws Exception {
 String endpoint="/api/v1/integrations/telegram/";
 String input=body("chatId","12345","userId","12345","url","https://example.org");
 mvc.perform(post(endpoint+"analyze").contentType("application/json").content(input)).andExpect(status().isUnauthorized());
 var result=mvc.perform(post(endpoint+"analyze").header("X-Sentinela-Telegram-Key","test-only-telegram-integration-key-32-chars").contentType("application/json").content(input)).andExpect(status().isOk()).andReturn().getResponse();
 String id=json.readTree(result.getContentAsString()).path("id").asText();
 mvc.perform(post(endpoint+"chat").header("X-Sentinela-Telegram-Key","test-only-telegram-integration-key-32-chars").contentType("application/json").content(body("chatId","67890","userId","67890","message","Explique","analysisId",id))).andExpect(status().isNotFound());
 mvc.perform(post(endpoint+"chat").header("X-Sentinela-Telegram-Key","test-only-telegram-integration-key-32-chars").contentType("application/json").content(body("chatId","12345","userId","12345","message","Explique","analysisId",id))).andExpect(status().isOk());
 mvc.perform(post(endpoint+"analyze").header("X-Sentinela-Telegram-Key","test-only-telegram-integration-key-32-chars").contentType("application/json").content(body("chatId","12345","userId","67890","url","https://example.org"))).andExpect(status().isForbidden());
 mvc.perform(post(endpoint+"erase").header("X-Sentinela-Telegram-Key","test-only-telegram-integration-key-32-chars").contentType("application/json").content(body("chatId","12345","userId","12345"))).andExpect(status().isOk());
 assertNull(s.one("SELECT id FROM analyses WHERE id=?",id));
 }
}
