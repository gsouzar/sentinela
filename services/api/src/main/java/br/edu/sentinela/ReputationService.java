package br.edu.sentinela;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReputationService {
  public record Source(String name, String status, String detail, String checkedAt) {}

  record Cached(long expires, List<Source> sources) {}

  final boolean demo;
  final String google, urlhaus;
  final ObjectMapper json;
  final Map<String, Cached> cache = new ConcurrentHashMap<>();
  final HttpClient http =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(3))
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();

  ReputationService(
      @Value("${sentinela.demo}") boolean demo,
      @Value("${sentinela.google-key}") String g,
      @Value("${sentinela.urlhaus-key}") String u,
      ObjectMapper j) {
    this.demo = demo;
    google = g;
    urlhaus = u;
    json = j;
  }

  public List<Source> check(String url, String domain) {
    var old = cache.get(url);
    if (old != null && old.expires > System.currentTimeMillis()) return old.sources;
    List<Source> list = new ArrayList<>();
    String time = Instant.now().toString();
    if (demo && domain.endsWith(".test")) {
      list.add(
          new Source(
              "Base de demonstração (sintética)",
              domain.equals("banc0-seguranca-login.test") ? "FOUND" : "NOT_FOUND",
              "Exemplo controlado para a apresentação; não é uma fonte real.",
              time));
    }
    list.add(
        provider(
            "Google Safe Browsing",
            google,
            () -> {
              String body =
                  json.writeValueAsString(
                      Map.of(
                          "client",
                          Map.of("clientId", "sentinela-academico", "clientVersion", "1.0"),
                          "threatInfo",
                          Map.of(
                              "threatTypes",
                              List.of("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE"),
                              "platformTypes",
                              List.of("ANY_PLATFORM"),
                              "threatEntryTypes",
                              List.of("URL"),
                              "threatEntries",
                              List.of(Map.of("url", url)))));
              var req =
                  HttpRequest.newBuilder(
                          URI.create(
                              "https://safebrowsing.googleapis.com/v4/threatMatches:find?key="
                                  + URLEncoder.encode(google, StandardCharsets.UTF_8)))
                      .timeout(Duration.ofSeconds(5))
                      .header("Content-Type", "application/json")
                      .POST(HttpRequest.BodyPublishers.ofString(body))
                      .build();
              return hasGoogleMatches(request(req));
            },
            time));
    list.add(
        provider(
            "URLhaus",
            urlhaus,
            () -> {
              var req =
                  HttpRequest.newBuilder(URI.create("https://urlhaus-api.abuse.ch/v1/url/"))
                      .timeout(Duration.ofSeconds(5))
                      .header("Auth-Key", urlhaus)
                      .header("Content-Type", "application/x-www-form-urlencoded")
                      .POST(
                          HttpRequest.BodyPublishers.ofString(
                              "url=" + URLEncoder.encode(url, StandardCharsets.UTF_8)))
                      .build();
              var result = request(req);
              String state = result.path("query_status").asText();
              if (!List.of("ok", "no_results").contains(state)) throw new IllegalStateException();
              return state.equals("ok");
            },
            time));
    if (cache.size() > 5000) cache.clear();
    var result = List.copyOf(list);
    cache.put(url, new Cached(System.currentTimeMillis() + 300000, result));
    return result;
  }

  static boolean hasGoogleMatches(com.fasterxml.jackson.databind.JsonNode response) {
    if (response == null || !response.isObject())
      throw new IllegalArgumentException("Resposta inválida da fonte");
    if (!response.has("matches")) return false;
    if (!response.path("matches").isArray())
      throw new IllegalArgumentException("Lista de correspondências inválida");
    return !response.path("matches").isEmpty();
  }

  interface Lookup {
    boolean found() throws Exception;
  }

  Source provider(String name, String key, Lookup lookup, String at) {
    if (key.isBlank())
      return new Source(name, "NOT_CONFIGURED", "Chave de acesso não configurada.", at);
    try {
      return new Source(
          name,
          lookup.found() ? "FOUND" : "NOT_FOUND",
          "Consulta de reputação do endereço sem query e fragmento.",
          at);
    } catch (Exception e) {
      return new Source(
          name,
          "UNAVAILABLE",
          "Não foi possível consultar a fonte. Nenhuma conclusão de segurança foi obtida.",
          at);
    }
  }

  com.fasterxml.jackson.databind.JsonNode request(HttpRequest req) throws Exception {
    var res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
    try (var in = res.body()) {
      byte[] bytes = in.readNBytes(65537);
      if (res.statusCode() != 200 || bytes.length > 65536) throw new IllegalStateException();
      return json.readTree(bytes);
    }
  }
}
