package br.edu.sentinela;

import java.net.*;
import java.util.*;

public class UrlAnalyzer {
  public record Indicator(String id, String title, String explanation, int weight) {}

  public record Result(
      String url,
      String domain,
      int score,
      String risk,
      String rulesVersion,
      List<Indicator> indicators) {}

  public Result analyze(String input) {
    if (input == null
        || input.isBlank()
        || input.length() > 2048
        || input.chars().anyMatch(c -> c < 33 || c == 127)
        || input.contains("\\")) throw bad();
    try {
      URI uri = new URI(input);
      String scheme = uri.getScheme();
      if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) throw bad();
      String authority = uri.getRawAuthority();
      if (authority == null
          || authority.contains("@")
          || authority.contains("%")
          || authority.contains("[")
          || authority.contains("]")) throw bad();
      String rawHost = authority;
      int colon = authority.lastIndexOf(':');
      if (colon >= 0) {
        String port = authority.substring(colon + 1);
        if (!port.matches("[0-9]{1,5}")
            || Integer.parseInt(port) > 65535
            || Integer.parseInt(port) < 1) throw bad();
        rawHost = authority.substring(0, colon);
      }
      String host = IDN.toASCII(rawHost, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
      if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
      if (host.length() > 253
          || !host.contains(".")
          || host.endsWith(".localhost")
          || host.endsWith(".local")
          || host.endsWith(".internal")
          || host.endsWith(".home")
          || host.endsWith(".lan")
          || host.endsWith(".invalid")) throw bad();
      boolean numeric =
          host.matches("[0-9.]+")
              || host.matches("(?i)(0x[0-9a-f]+|[0-9]+)(\\.(0x[0-9a-f]+|[0-9]+))*");
      if (numeric) {
        String[] o = host.split("\\.");
        if (o.length != 4) throw bad();
        int[] v = new int[4];
        for (int i = 0; i < 4; i++) {
          if (o[i].length() > 1 && o[i].startsWith("0")) throw bad();
          v[i] = Integer.parseInt(o[i]);
          if (v[i] > 255) throw bad();
        }
        if (v[0] == 0
            || v[0] == 10
            || v[0] == 127
            || v[0] >= 224
            || v[0] == 169 && v[1] == 254
            || v[0] == 172 && v[1] >= 16 && v[1] <= 31
            || v[0] == 192 && v[1] == 168
            || v[0] == 100 && v[1] >= 64 && v[1] <= 127
            || v[0] == 198 && (v[1] == 18 || v[1] == 19)
            || v[0] == 192 && v[1] == 0
            || v[0] == 198 && v[1] == 51 && v[2] == 100
            || v[0] == 203 && v[1] == 0 && v[2] == 113) throw bad();
      }
      List<Indicator> items = new ArrayList<>();
      if (!scheme.equalsIgnoreCase("https"))
        items.add(
            new Indicator(
                "HTTP",
                "Conexão sem HTTPS",
                "O endereço não solicita uma conexão HTTPS. Isso isoladamente não comprova fraude.",
                10));
      if (numeric)
        items.add(
            new Indicator(
                "IP",
                "Endereço usa um IP",
                "O endereço utiliza IP no lugar de um nome de domínio.",
                20));
      if (host.contains("xn--"))
        items.add(
            new Indicator(
                "IDN",
                "Domínio internacionalizado",
                "Caracteres internacionais podem se parecer com letras de outro domínio. Verifique"
                    + " a grafia.",
                15));
      if (host.split("\\.").length > 4)
        items.add(
            new Indicator(
                "SUBDOMAINS",
                "Muitos subdomínios",
                "A estrutura longa pode dificultar identificar o domínio principal.",
                10));
      if (input.length() > 180)
        items.add(
            new Indicator(
                "LENGTH",
                "Endereço extenso",
                "Um link longo dificulta sua leitura; esse indicador também aparece em links"
                    + " legítimos.",
                10));
      String content =
          (host + Optional.ofNullable(uri.getPath()).orElse("")).toLowerCase(Locale.ROOT);
      boolean sensitive =
          content.matches(".*(login|confirmar|verificar|urgente|senha|seguranca|atualiz).*");
      if (sensitive)
        items.add(
            new Indicator(
                "TERMS",
                "Termos sensíveis",
                "O endereço contém termos relacionados a acesso, confirmação ou urgência.",
                10));
      if (sensitive
          && host.matches(
              ".*(banc0|paypa[l1]-|g[o0]{2}g[l1]e-|m[i1]crosoft-|itau-|nubank-|bradesco-).*"))
        items.add(
            new Indicator(
                "IMITATION",
                "Possível imitação de marca",
                "O domínio combina referência a uma marca e termos sensíveis. Confira o endereço"
                    + " oficial por outro canal.",
                25));
      int score = Math.min(100, items.stream().mapToInt(Indicator::weight).sum());
      String path = Optional.ofNullable(uri.getRawPath()).filter(x -> !x.isBlank()).orElse("/");
      return new Result(
          scheme.toLowerCase(Locale.ROOT)
              + "://"
              + host
              + (colon >= 0 ? authority.substring(colon) : "")
              + path,
          host,
          score,
          risk(score),
          "rules-1.0",
          List.copyOf(items));
    } catch (Exception ex) {
      throw bad();
    }
  }

  public static String risk(int s) {
    return s >= 60 ? "HIGH" : s >= 30 ? "MEDIUM" : "LOW";
  }

  private IllegalArgumentException bad() {
    return new IllegalArgumentException(
        "Informe uma URL HTTP/HTTPS pública válida, sem credenciais e com até 2048 caracteres. IPv6"
            + " não é aceito nesta versão.");
  }
}
