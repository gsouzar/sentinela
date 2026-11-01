package br.edu.sentinela;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AnalysisController {
  final Store s;
  final ReputationService reputation;
  final UrlAnalyzer analyzer = new UrlAnalyzer();

  AnalysisController(Store s, ReputationService r) {
    this.s = s;
    reputation = r;
  }

  public record Input(@NotBlank @Size(max = 2048) String url) {}

  @PostMapping("/analyses")
  @ResponseStatus(HttpStatus.CREATED)
  Object analyze(@AuthenticationPrincipal SessionFilter.Viewer v, @Valid @RequestBody Input input) {
    s.limit("analysis:" + v.id(), 30, 60);
    var local = analyzer.analyze(input.url().strip());
    var sources = reputation.check(local.url(), local.domain());
    List<UrlAnalyzer.Indicator> indicators = new ArrayList<>(local.indicators());
    boolean hit = sources.stream().anyMatch(x -> x.status().equals("FOUND"));
    int score = hit ? Math.max(80, local.score()) : local.score();
    if (hit)
      indicators.add(
          new UrlAnalyzer.Indicator(
              "REPUTATION",
              "Registro em base de ameaças",
              "Uma fonte retornou correspondência. Confira se é uma fonte real ou exemplo sintético"
                  + " na lista de fontes.",
              Math.max(0, 80 - local.score())));
    boolean partial =
        sources.stream()
            .anyMatch(x -> List.of("NOT_CONFIGURED", "UNAVAILABLE").contains(x.status()));
    String id = UUID.randomUUID().toString();
    long now = System.currentTimeMillis();
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("id", id);
    out.put("url", local.url());
    out.put("domain", local.domain());
    out.put("score", score);
    out.put("risk", UrlAnalyzer.risk(score));
    out.put("rulesVersion", local.rulesVersion());
    out.put("indicators", indicators);
    out.put("sources", sources);
    out.put("coverage", partial ? "PARTIAL" : "COMPLETE");
    out.put("createdAt", Instant.ofEpochMilli(now).toString());
    out.put(
        "notice",
        "Score não representa probabilidade. Baixo risco não garante segurança. Query e fragmento"
            + " foram removidos antes de persistir e consultar reputação.");
    s.jdbc()
        .update(
            "INSERT INTO analyses VALUES(?,?,?,?,?,?,?,?)",
            id,
            v.id(),
            local.domain(),
            local.url(),
            score,
            UrlAnalyzer.risk(score),
            s.encode(out),
            now);
    s.audit(v.id(), "ANALYSIS_CREATED", "Avaliação " + id);
    return out;
  }

  public Map<String, Object> find(SessionFilter.Viewer v, String id) {
    var row =
        s.one(
            "SELECT * FROM analyses WHERE id=? AND (user_id=? OR ?='ADMIN')", id, v.id(), v.role());
    if (row == null) throw new ApiError(404, "Análise não encontrada.");
    return s.decode(row.get("result_json").toString());
  }

  @GetMapping("/analyses/{id}")
  Object details(@AuthenticationPrincipal SessionFilter.Viewer v, @PathVariable String id) {
    return find(v, id);
  }

  @DeleteMapping("/analyses/{id}")
  Object delete(@AuthenticationPrincipal SessionFilter.Viewer v, @PathVariable String id) {
    int n = s.jdbc().update("DELETE FROM analyses WHERE id=? AND user_id=?", id, v.id());
    if (n == 0) throw new ApiError(404, "Análise não encontrada.");
    s.jdbc().update("DELETE FROM chat_messages WHERE user_id=? AND analysis_id=?", v.id(), id);
    s.audit(v.id(), "ANALYSIS_DELETED", "Análise removida");
    return Map.of("ok", true);
  }

  @GetMapping("/analyses")
  Object list(
      @AuthenticationPrincipal SessionFilter.Viewer v,
      @RequestParam(defaultValue = "") String risk,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "15") int size,
      @RequestParam(defaultValue = "30") int days) {
    if (!risk.isBlank() && !List.of("LOW", "MEDIUM", "HIGH").contains(risk)
        || page < 0
        || page > 10000
        || size < 1
        || size > 100
        || days < 1
        || days > 3650
        || q.length() > 100) throw new ApiError(400, "Filtros inválidos.");
    String where =
        " WHERE (user_id=? OR ?='ADMIN') AND (?='' OR risk=?) AND LOWER(domain) LIKE ? ESCAPE '!'"
            + " AND created_at>=?";
    String pattern =
        "%"
            + q.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_")
            + "%";
    long since = System.currentTimeMillis() - days * 86400000L;
    var args = new Object[] {v.id(), v.role(), risk, risk, pattern, since};
    long total = s.jdbc().queryForObject("SELECT COUNT(*) FROM analyses" + where, Long.class, args);
    var params = new ArrayList<>(Arrays.asList(args));
    params.add(size);
    params.add(page * size);
    var items =
        s.rows(
            "SELECT id,domain,url,score,risk,created_at FROM analyses"
                + where
                + " ORDER BY created_at DESC LIMIT ? OFFSET ?",
            params.toArray());
    return Map.of("items", items, "total", total, "page", page, "size", size);
  }

  @GetMapping("/dashboard")
  Object dashboard(
      @AuthenticationPrincipal SessionFilter.Viewer v, @RequestParam(defaultValue = "7") int days) {
    if (days < 1 || days > 365) throw new ApiError(400, "Período inválido.");
    var rows =
        s.rows(
            "SELECT score,risk,created_at FROM analyses WHERE (user_id=? OR ?='ADMIN') AND"
                + " created_at>=?",
            v.id(),
            v.role(),
            System.currentTimeMillis() - days * 86400000L);
    Map<String, Long> risks = new LinkedHashMap<>();
    for (String r : List.of("LOW", "MEDIUM", "HIGH"))
      risks.put(r, rows.stream().filter(x -> r.equals(x.get("risk"))).count());
    List<Map<String, Object>> trend = new ArrayList<>();
    for (int i = days - 1; i >= 0; i--) {
      var date = LocalDate.now(ZoneOffset.UTC).minusDays(i);
      long start = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
          end = start + 86400000;
      var daily =
          rows.stream()
              .filter(
                  x ->
                      ((Number) x.get("created_at")).longValue() >= start
                          && ((Number) x.get("created_at")).longValue() < end)
              .toList();
      trend.add(
          Map.of(
              "date",
              date.toString(),
              "total",
              daily.size(),
              "high",
              daily.stream().filter(x -> "HIGH".equals(x.get("risk"))).count()));
    }
    return Map.of(
        "total",
        rows.size(),
        "risks",
        risks,
        "trend",
        trend,
        "days",
        days,
        "scope",
        v.role().equals("ADMIN") ? "Todas as contas" : "Minha conta",
        "updatedAt",
        Instant.now().toString());
  }

  @GetMapping("/admin/audit")
  Object audit(@RequestParam(defaultValue = "0") int page) {
    if (page < 0 || page > 10000) throw new ApiError(400, "Página inválida.");
    return s.rows(
        "SELECT id,user_id,action,detail,created_at FROM audit_events ORDER BY created_at DESC"
            + " LIMIT 50 OFFSET ?",
        page * 50);
  }

  @GetMapping("/admin/users")
  Object users() {
    return s.rows(
        "SELECT id,name,email,role,enabled,mfa_enabled,created_at FROM app_users ORDER BY"
            + " created_at DESC LIMIT 200");
  }

  public record UserChange(@NotNull Boolean enabled) {}

  @PatchMapping("/admin/users/{id}")
  Object user(
      @AuthenticationPrincipal SessionFilter.Viewer v,
      @PathVariable String id,
      @Valid @RequestBody UserChange b) {
    if (v.id().equals(id)) throw new ApiError(400, "Não é possível desativar sua própria conta.");
    int n =
        s.jdbc()
            .update("UPDATE app_users SET enabled=?,subscription_status=CASE WHEN ? THEN 'ACTIVE' ELSE 'SUSPENDED' END WHERE id=? AND role<>'ADMIN'", b.enabled(), b.enabled(), id);
    if (n == 0) throw new ApiError(404, "Conta comum não encontrada.");
    if (!b.enabled()) s.jdbc().update("DELETE FROM sessions WHERE user_id=?", id);
    s.audit(v.id(), "USER_UPDATED", "Conta " + id + " ativa=" + b.enabled());
    return Map.of("ok", true);
  }
}
