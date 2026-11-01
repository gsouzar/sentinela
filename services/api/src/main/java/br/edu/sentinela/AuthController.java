package br.edu.sentinela;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
  final Store s;
  final Crypto c;
  final PasswordEncoder passwords;
  final boolean secure;
  final String dummy;
  @Value("${sentinela.require-mfa:false}") boolean requireMfa;
  boolean required(Object role) { return requireMfa || "ADMIN".equals(role); }
  Map<String,Object> lockViewer(SessionFilter.Viewer v) {
    var u=s.one("SELECT * FROM app_users WHERE id=? FOR UPDATE",v.id());
    if(u==null || !Boolean.TRUE.equals(u.get("enabled")) || s.one("SELECT id FROM sessions WHERE id=? AND user_id=? AND expires_at>?",v.sessionId(),v.id(),System.currentTimeMillis())==null) throw new ApiError(401,"Sessão encerrada. Entre novamente.");
    return u;
  }

  AuthController(
      Store s, Crypto c, PasswordEncoder p, @Value("${sentinela.secure-cookies}") boolean secure) {
    this.s = s;
    this.c = c;
    passwords = p;
    this.secure = secure;
    dummy = p.encode(c.token());
  }

  public record Register(
      @NotBlank @Size(max = 80) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @NotNull @Size(min = 12, max = 128) String password) {}

  public record Login(
      @NotBlank @Email @Size(max = 254) String email,
      @NotNull @Size(max = 128) String password,
      @Size(max = 40) String code) {}

  public record Proof(@NotNull @Size(max = 128) String password, @Size(max = 40) String code) {}

  @GetMapping("/health")
  Map<String, Object> health() {
    return Map.of("status", "UP", "service", "Sentinela");
  }

  @GetMapping("/csrf")
  Map<String, Object> csrf(CsrfToken token) {
    return Map.of("token", token.getToken());
  }

  @PostMapping("/auth/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public Map<String, Object> register(
      @Valid @RequestBody Register b, HttpServletRequest req, HttpServletResponse res) {
    s.limit("register:" + req.getRemoteAddr(), 10, 3600);
    String email = b.email().toLowerCase(Locale.ROOT).strip();
    if (b.name().chars().anyMatch(x -> x < 32)) throw new ApiError(400, "Nome inválido.");
    String id = UUID.randomUUID().toString();
    try {
      s.jdbc()
          .update(
              "INSERT INTO app_users(id,name,email,password_hash,role,created_at)"
                  + " VALUES(?,?,?,?,?,?)",
              id,
              b.name().strip(),
              email,
              passwords.encode(b.password()),
              "USER",
              System.currentTimeMillis());
    } catch (org.springframework.dao.DuplicateKeyException e) {
      throw new ApiError(409, "Não foi possível cadastrar esse email. Se já tiver conta, entre.");
    }
    s.audit(id, "REGISTER", "Conta criada");
    return issue(id, req, res);
  }

  @PostMapping("/auth/login")
  @Transactional
  public Map<String, Object> login(
      @Valid @RequestBody Login b, HttpServletRequest req, HttpServletResponse res) {
    s.limit("login-ip:" + req.getRemoteAddr(), 30, 900);
    s.limit("login-user:" + Crypto.hash(b.email().toLowerCase(Locale.ROOT)), 10, 900);
    var u =
        s.one("SELECT * FROM app_users WHERE email=? FOR UPDATE", b.email().toLowerCase(Locale.ROOT).strip());
    boolean ok =
        passwords.matches(b.password(), u == null ? dummy : u.get("password_hash").toString());
    if (!ok || u == null || !Boolean.TRUE.equals(u.get("enabled")) || "BOT".equals(u.get("role"))) {
      s.failedAuthentication(null, "Credenciais recusadas");
      throw new ApiError(401, "Email ou senha inválidos.");
    }
    if (Boolean.TRUE.equals(u.get("mfa_enabled"))) {
      if (b.code() == null || b.code().isBlank())
        throw new ApiError(428, "Informe o código do autenticador ou um código de recuperação.");
      try { verifyMfa(u, b.code()); }
      catch (ApiError failure) {
        s.failedAuthentication(u.get("id").toString(), "Segundo fator recusado");
        throw failure;
      }
    }
    if(passwords.upgradeEncoding(u.get("password_hash").toString())) s.jdbc().update("UPDATE app_users SET password_hash=? WHERE id=?",passwords.encode(b.password()),u.get("id"));
    s.audit(u.get("id").toString(), "LOGIN", "Autenticação concluída");
    return issue(u.get("id").toString(), req, res);
  }

  Map<String, Object> issue(String user, HttpServletRequest req, HttpServletResponse res) {
    String access = c.token(), refresh = c.token(), id = UUID.randomUUID().toString();
    long now = System.currentTimeMillis();
    boolean android = "android".equals(req.getHeader("X-Sentinela-Client"));
    s.jdbc().update("DELETE FROM sessions WHERE refresh_expires<?", now);
    s.jdbc()
        .update(
            "INSERT INTO sessions VALUES(?,?,?,?,?,?,?)",
            id,
            user,
            Crypto.hash(access),
            android ? Crypto.hash(refresh) : null,
            now + (android ? 900000 : 28800000),
            now + 604800000,
            now);
    var u = s.one("SELECT id,name,email,role,mfa_enabled FROM app_users WHERE id=?", user);
    Map<String, Object> out = new LinkedHashMap<>();
    u.put("mfa_required", required(u.get("role")));
    out.put("user", u);
    if (android) {
      out.put("accessToken", access);
      out.put("refreshToken", refresh);
    } else
      res.addHeader(
          "Set-Cookie",
          ResponseCookie.from("SENTINELA_SESSION", access)
              .httpOnly(true)
              .secure(secure)
              .sameSite("Strict")
              .path("/")
              .maxAge(Duration.ofHours(8))
              .build()
              .toString());
    return out;
  }

  @PostMapping("/auth/refresh")
  @Transactional(noRollbackFor = ApiError.class)
  public Map<String, Object> refresh(
      @RequestBody Map<String, String> b, HttpServletRequest req, HttpServletResponse res) {
    if (!"android".equals(req.getHeader("X-Sentinela-Client")))
      throw new ApiError(400, "Cliente inválido.");
    s.limit("refresh:" + req.getRemoteAddr(), 60, 900);
    String raw = b.getOrDefault("refreshToken", "");
    if (raw.length() > 200) throw new ApiError(400, "Token inválido.");
    String hash = Crypto.hash(raw);
    var owner=s.one("SELECT user_id FROM sessions WHERE refresh_hash=? UNION SELECT user_id FROM consumed_refresh WHERE token_hash=?",hash,hash);
    if(owner==null) throw new ApiError(401,"Sessão expirada.");
    var locked=s.one("SELECT id,enabled FROM app_users WHERE id=? FOR UPDATE",owner.get("user_id"));
    if(locked==null || !Boolean.TRUE.equals(locked.get("enabled"))) throw new ApiError(401,"Sessão expirada.");
    var old =
        s.one(
            "SELECT * FROM consumed_refresh WHERE token_hash=? AND expires_at>?",
            hash,
            System.currentTimeMillis());
    if (old != null) {
      s.jdbc().update("DELETE FROM sessions WHERE user_id=?", old.get("user_id"));
      s.audit(old.get("user_id").toString(), "REFRESH_REPLAY", "Sessões revogadas");
      throw new ApiError(401, "Sessão revogada. Entre novamente.");
    }
    var row =
        s.one(
            "SELECT s.* FROM sessions s JOIN app_users u ON u.id=s.user_id WHERE s.refresh_hash=?"
                + " AND s.refresh_expires>? AND u.enabled=TRUE",
            hash,
            System.currentTimeMillis());
    if (row == null) throw new ApiError(401, "Sessão expirada.");
    int claimed =
        s.jdbc().update("DELETE FROM sessions WHERE id=? AND refresh_hash=?", row.get("id"), hash);
    if (claimed == 0) throw new ApiError(401, "Token já utilizado.");
    s.jdbc().update("DELETE FROM consumed_refresh WHERE expires_at<?", System.currentTimeMillis());
    s.jdbc()
        .update(
            "INSERT INTO consumed_refresh VALUES(?,?,?)",
            hash,
            row.get("user_id"),
            row.get("refresh_expires"));
    return issue(row.get("user_id").toString(), req, res);
  }

  @GetMapping("/me")
  Map<String, Object> me(@AuthenticationPrincipal SessionFilter.Viewer v) {
    var u=s.one("SELECT id,name,email,role,mfa_enabled,created_at FROM app_users WHERE id=?", v.id());
    u.put("mfa_required", required(u.get("role"))); return u;
  }

  @PostMapping("/me/sessions/revoke-all")
  @Transactional
  Object revokeAll(@AuthenticationPrincipal SessionFilter.Viewer v) {
    lockViewer(v);
    s.jdbc().update("DELETE FROM sessions WHERE user_id=?", v.id());
    s.audit(v.id(), "SESSIONS_REVOKED", "Todas as sessões encerradas");
    return Map.of("ok", true);
  }

  @GetMapping("/me/sessions")
  Object sessions(@AuthenticationPrincipal SessionFilter.Viewer v) {
    return s.rows(
        "SELECT id,created_at,expires_at FROM sessions WHERE user_id=? ORDER BY created_at DESC",
        v.id());
  }

  @DeleteMapping("/me/sessions/{id}")
  @Transactional
  Object revoke(@AuthenticationPrincipal SessionFilter.Viewer v, @PathVariable String id) {
    lockViewer(v);
    s.jdbc().update("DELETE FROM sessions WHERE user_id=? AND id=?", v.id(), id);
    s.audit(v.id(), "SESSION_REVOKED", "Sessão encerrada");
    return Map.of("ok", true);
  }

  @PostMapping("/auth/logout")
  @Transactional
  Object logout(@AuthenticationPrincipal SessionFilter.Viewer v, HttpServletResponse res) {
    lockViewer(v);
    s.jdbc().update("DELETE FROM sessions WHERE id=?", v.sessionId());
    res.addHeader(
        "Set-Cookie",
        ResponseCookie.from("SENTINELA_SESSION", "")
            .path("/")
            .httpOnly(true)
            .secure(secure)
            .sameSite("Strict")
            .maxAge(0)
            .build()
            .toString());
    s.audit(v.id(), "LOGOUT", "Sessão encerrada");
    return Map.of("ok", true);
  }

  void password(String id, String password) {
    var u = s.one("SELECT * FROM app_users WHERE id=?", id);
    if (password == null || !passwords.matches(password, u.get("password_hash").toString()))
      throw new ApiError(403, "Senha inválida.");
  }

  void verifyMfa(Map<String, Object> u, String code) {
    String id = u.get("id").toString();
    long last = ((Number) u.get("last_totp")).longValue();
    long t = c.verifyTotp(c.decrypt(u.get("mfa_secret").toString()), code, last);
    if (t >= 0
        && s.jdbc().update("UPDATE app_users SET last_totp=? WHERE id=? AND last_totp<?", t, id, t)
            == 1) return;
    if (code != null
        && s.jdbc()
                .update(
                    "DELETE FROM recovery_codes WHERE user_id=? AND code_hash=?",
                    id,
                    Crypto.hash(code.strip()))
            == 1) {
      s.audit(id, "MFA_RECOVERY", "Código de recuperação utilizado");
      return;
    }
    throw new ApiError(401, "Código inválido ou já utilizado.");
  }

  void verifyMfaProof(Map<String, Object> user, String code) {
    try {
      verifyMfa(user, code);
    } catch (ApiError e) {
      if (e.status == 401) throw new ApiError(403, e.getMessage());
      throw e;
    }
  }

  @PostMapping("/me/mfa/setup")
  @Transactional
  Object setup(@AuthenticationPrincipal SessionFilter.Viewer v, @Valid @RequestBody Proof b) {
    s.limit("mfa:" + v.id(), 10, 900);
    var u=lockViewer(v);
    password(v.id(), b.password());
    if (Boolean.TRUE.equals(u.get("mfa_enabled"))) throw new ApiError(409, "Segundo fator já está ativo.");
    String secret = c.newTotpSecret();
    s.jdbc()
        .update(
            "UPDATE app_users SET mfa_secret=?,last_totp=-1 WHERE id=? AND mfa_enabled=FALSE",
            c.encrypt(secret),
            v.id());
    return Map.of(
        "secret",
        secret,
        "uri",
        "otpauth://totp/Sentinela:"
            + java.net.URLEncoder.encode(v.email(), java.nio.charset.StandardCharsets.UTF_8)
            + "?secret="
            + secret
            + "&issuer=Sentinela");
  }

  @PostMapping("/me/mfa/enable")
  @Transactional
  Object enable(@AuthenticationPrincipal SessionFilter.Viewer v, @Valid @RequestBody Proof b) {
    s.limit("mfa:" + v.id(), 10, 900);
    var u=lockViewer(v);
    password(v.id(), b.password());
    if (Boolean.TRUE.equals(u.get("mfa_enabled")) || u.get("mfa_secret") == null)
      throw new ApiError(409, "Inicie a configuração do segundo fator.");
    verifyMfaProof(u, b.code());
    s.jdbc().update("UPDATE app_users SET mfa_enabled=TRUE WHERE id=?", v.id());
    s.jdbc().update("DELETE FROM recovery_codes WHERE user_id=?", v.id());
    List<String> codes = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      String code = c.token().substring(0, 16);
      codes.add(code);
      s.jdbc().update("INSERT INTO recovery_codes VALUES(?,?)", v.id(), Crypto.hash(code));
    }
    s.jdbc().update("DELETE FROM sessions WHERE user_id=? AND id<>?", v.id(), v.sessionId());
    s.audit(v.id(), "MFA_ENABLED", "Segundo fator ativado");
    return Map.of("recoveryCodes", codes);
  }

  @PostMapping("/me/mfa/disable")
  @Transactional
  Object disable(@AuthenticationPrincipal SessionFilter.Viewer v, @Valid @RequestBody Proof b) {
    s.limit("mfa:" + v.id(), 10, 900);
    var u=lockViewer(v);
    password(v.id(), b.password());
    if(required(u.get("role"))) throw new ApiError(403,"O segundo fator é obrigatório nesta conta.");
    if (!Boolean.TRUE.equals(u.get("mfa_enabled")))
      throw new ApiError(409, "Segundo fator não está ativo.");
    verifyMfaProof(u, b.code());
    s.jdbc()
        .update(
            "UPDATE app_users SET mfa_enabled=FALSE,mfa_secret=NULL,last_totp=-1 WHERE id=?",
            v.id());
    s.jdbc().update("DELETE FROM recovery_codes WHERE user_id=?", v.id());
    s.jdbc().update("DELETE FROM sessions WHERE user_id=? AND id<>?", v.id(), v.sessionId());
    s.audit(v.id(), "MFA_DISABLED", "Segundo fator desativado");
    return Map.of("ok", true);
  }

  public record ChangePassword(
      @NotNull @Size(max = 128) String password,
      @NotNull @Size(min = 12, max = 128) String newPassword,
      @Size(max = 40) String code) {}

  @PostMapping("/me/password")
  @Transactional
  Object change(
      @AuthenticationPrincipal SessionFilter.Viewer v, @Valid @RequestBody ChangePassword b) {
    s.limit("password:" + v.id(), 5, 900);
    var u=lockViewer(v);
    password(v.id(), b.password());
    if (Boolean.TRUE.equals(u.get("mfa_enabled"))) verifyMfaProof(u, b.code());
    s.jdbc()
        .update(
            "UPDATE app_users SET password_hash=? WHERE id=?",
            passwords.encode(b.newPassword()),
            v.id());
    s.jdbc().update("DELETE FROM sessions WHERE user_id=?", v.id());
    s.audit(v.id(), "PASSWORD_CHANGED", "Sessões encerradas");
    return Map.of("ok", true);
  }
}
