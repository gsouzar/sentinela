package br.edu.sentinela;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
  final Store s;
  final AnalysisController analysis;
  final String endpoint, key;
  final HttpClient http =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(3))
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();

  ChatController(
      Store s,
      AnalysisController a,
      @Value("${sentinela.colab-url}") String e,
      @Value("${sentinela.colab-key}") String k) {
    this.s = s;
    analysis = a;
    endpoint = e;
    key = k;
  }

  public record Ask(
      @NotBlank @Size(max = 1500) String message, @Size(max = 36) String analysisId) {}

  @PostMapping
  public Object ask(@AuthenticationPrincipal SessionFilter.Viewer v, @Valid @RequestBody Ask b) {
    s.limit("chat:" + v.id(), 15, 60);
    Map<String, Object> result =
        b.analysisId() == null || b.analysisId().isBlank()
            ? null
            : analysis.find(v, b.analysisId());
    String answer;
    List<String> evidence = new ArrayList<>();
    if (result != null) {
      var items = (List<Map<String, Object>>) result.get("indicators");
      String level =
          switch (result.get("risk").toString()) {
            case "HIGH" -> "alto";
            case "MEDIUM" -> "médio";
            default -> "baixo";
          };
      StringBuilder text =
          new StringBuilder(
              "A análise de "
                  + result.get("domain")
                  + " recebeu risco "
                  + level
                  + ", com score "
                  + result.get("score")
                  + "/100. Essa pontuação não é uma probabilidade.\n\n");
      for (var i : items) {
        evidence.add(i.get("id").toString());
        text.append("• ")
            .append(i.get("title"))
            .append(": ")
            .append(i.get("explanation"))
            .append("\n");
      }
      if (items.isEmpty())
        text.append(
            "Não encontramos indicadores nas regras locais. Isso não garante que o endereço seja"
                + " seguro.\n");
      text.append("\n")
          .append(
              result.get("coverage").equals("PARTIAL")
                  ? "A análise é parcial: algumas fontes externas não responderam ou não estão"
                      + " configuradas. "
                  : "")
          .append(
              "Não informe senhas ou dados pessoais em páginas suspeitas. Abra o aplicativo oficial"
                  + " ou digite o endereço conhecido da instituição.");
      answer = text.toString();
    } else {
      String q = b.message().toLowerCase(Locale.ROOT);
      answer =
          q.contains("pix")
              ? "Antes de fazer um Pix, confirme nome e instituição do recebedor no aplicativo do"
                  + " banco. Desconfie de pressão por urgência. Se já transferiu em uma possível"
                  + " fraude, procure imediatamente seu banco por um canal oficial."
              : q.contains("senha")
                  ? "Use uma senha longa e exclusiva para cada conta, prefira um gerenciador de"
                      + " senhas e ative o segundo fator. Nunca compartilhe códigos do"
                      + " autenticador ou de recuperação."
                  : q.contains("cliquei")
                      ? "Se apenas abriu o link, feche a página e não faça downloads. Se informou"
                          + " uma senha, altere-a no aplicativo oficial, encerre sessões e ative"
                          + " o segundo fator. Se informou dados bancários, procure seu banco por"
                          + " um canal oficial."
                      : "Phishing é uma tentativa de enganar você para obter dados ou dinheiro."
                          + " Confira o domínio, desconfie de urgência e confirme pedidos em"
                          + " canais oficiais. Para uma explicação específica, selecione uma"
                          + " análise. Um score baixo não garante segurança.";
    }
    String mode = "LOCAL";
    if (!endpoint.isBlank() && !key.isBlank() && result != null) {
      try {
        if (!endpoint.startsWith("https://")) throw new IllegalStateException();
        var payload = Map.of("question", b.message(), "analysis", result);
        var req =
            HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/json")
                .header("X-Sentinela-Key", key)
                .POST(HttpRequest.BodyPublishers.ofString(s.encode(payload)))
                .build();
        var res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
        try (var in = res.body()) {
          byte[] bytes = in.readNBytes(16385);
          if (res.statusCode() != 200 || bytes.length > 16384) throw new IllegalStateException();
          var generated = s.decode(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
          var refs = (List<?>) generated.get("evidenceIds");
          String text = Objects.toString(generated.get("answer"), "");
          if (refs == null
              || refs.isEmpty() && !evidence.isEmpty()
              || !evidence.containsAll(refs)
              || text.isBlank()
              || text.length() > 6000) throw new IllegalStateException();
          answer = answer + "\n\nExplicação complementar do Colab:\n" + text;
          mode = "COLAB";
        }
      } catch (Exception ignored) {
        mode = "LOCAL_FALLBACK";
      }
    }
    String id = UUID.randomUUID().toString();
    long now = System.currentTimeMillis();
    s.jdbc()
        .update(
            "INSERT INTO chat_messages VALUES(?,?,?,?,?,?,?)",
            id,
            v.id(),
            b.analysisId(),
            redact(b.message()),
            answer,
            mode,
            now);
    return Map.of(
        "id",
        id,
        "answer",
        answer,
        "mode",
        mode,
        "evidenceIds",
        evidence,
        "createdAt",
        Instant.ofEpochMilli(now).toString());
  }

  String redact(String x) {
    return x.replaceAll("https?://[^\\s]+", "[link removido do histórico]");
  }

  @GetMapping
  Object history(@AuthenticationPrincipal SessionFilter.Viewer v) {
    return s.rows(
        "SELECT id,analysis_id,question,answer,mode,created_at FROM chat_messages WHERE user_id=?"
            + " ORDER BY created_at DESC LIMIT 50",
        v.id());
  }

  @DeleteMapping
  Object clear(@AuthenticationPrincipal SessionFilter.Viewer v) {
    s.jdbc().update("DELETE FROM chat_messages WHERE user_id=?", v.id());
    return Map.of("ok", true);
  }
}
