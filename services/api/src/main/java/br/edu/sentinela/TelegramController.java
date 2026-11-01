package br.edu.sentinela;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/** Dedicated machine integration. Never accepts a human account/session identifier. */
@RestController @RequestMapping("/api/v1/integrations/telegram")
public class TelegramController {
 final Store s; final AnalysisController analysis; final ChatController chat; final PasswordEncoder passwords; final Crypto crypto; final String key;
 TelegramController(Store s,AnalysisController a,ChatController c,PasswordEncoder p,Crypto crypto,@Value("${sentinela.telegram-api-key:}") String key){this.s=s;analysis=a;chat=c;passwords=p;this.crypto=crypto;this.key=key; if(!key.isBlank() && key.length()<32) throw new IllegalArgumentException("TELEGRAM_API_KEY precisa de pelo menos 32 caracteres aleatórios.");}
 public record Message(@NotBlank @Pattern(regexp="[1-9][0-9]{0,19}") String chatId,@NotBlank @Pattern(regexp="[1-9][0-9]{0,19}") String userId,@Size(max=2048) String url,@Size(max=1500) String message,@Size(max=36) String analysisId){}
 SessionFilter.Viewer actor(HttpServletRequest req,Message b){
  if(key.isBlank()) throw new ApiError(503,"Integração Telegram desativada.");
  String supplied=req.getHeader("X-Sentinela-Telegram-Key");
  if(supplied==null || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8))) throw new ApiError(401,"Integração não autorizada.");
  if(req.getHeader("Authorization")!=null || req.getCookies()!=null) throw new ApiError(400,"Não misture credenciais da integração e da conta.");
  if(!b.chatId().equals(b.userId())) throw new ApiError(403,"Use uma conversa privada com o bot.");
  s.limit("telegram-global",120,60);
  String id;
  try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));byte[] digest=mac.doFinal(("telegram|"+b.chatId()+"|"+b.userId()).getBytes(StandardCharsets.UTF_8));var bytes=java.nio.ByteBuffer.wrap(digest);id=new UUID(bytes.getLong(),bytes.getLong()).toString();}catch(Exception e){throw new IllegalStateException(e);}
  s.limit("telegram:"+id,15,60);
  if(s.one("SELECT id FROM app_users WHERE id=?",id)==null) s.jdbc().update("INSERT INTO app_users(id,name,email,password_hash,role,created_at) VALUES(?,?,?,?,?,?) ON CONFLICT DO NOTHING",id,"Telegram (conversa privada)",id+"@telegram.invalid",passwords.encode(crypto.token()),"BOT",System.currentTimeMillis());
  var user=s.one("SELECT role,enabled FROM app_users WHERE id=?",id);
  if(user==null || !"BOT".equals(user.get("role")) || !Boolean.TRUE.equals(user.get("enabled"))) throw new ApiError(403,"Integração indisponível para esta conversa.");
  return new SessionFilter.Viewer(id,"Telegram","", "BOT","integration",false);
 }
 @PostMapping("/analyze") Object analyze(HttpServletRequest req,@Valid @RequestBody Message b){var v=actor(req,b);if(b.url()==null || b.url().isBlank())throw new ApiError(400,"Informe o link.");return analysis.analyze(v,new AnalysisController.Input(b.url()));}
 @PostMapping("/chat") Object ask(HttpServletRequest req,@Valid @RequestBody Message b){var v=actor(req,b);if(b.message()==null || b.message().isBlank())throw new ApiError(400,"Informe a pergunta.");return chat.ask(v,new ChatController.Ask(b.message(),b.analysisId()));}
 @PostMapping("/erase") Object erase(HttpServletRequest req,@Valid @RequestBody Message b){var v=actor(req,b);s.jdbc().update("DELETE FROM chat_messages WHERE user_id=?",v.id());s.jdbc().update("DELETE FROM analyses WHERE user_id=?",v.id());s.audit(v.id(),"TELEGRAM_ERASE","Dados da conversa removidos");return Map.of("ok",true);}
}
