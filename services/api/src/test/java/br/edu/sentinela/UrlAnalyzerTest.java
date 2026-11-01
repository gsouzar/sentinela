package br.edu.sentinela;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UrlAnalyzerTest {
  @Test
  void rejectsLocalAndAmbiguousDestinations() {
    for (String s :
        new String[] {
          "http://127.0.0.1",
          "http://2130706433",
          "http://[::1]",
          "http://localhost",
          "http://10.0.0.1",
          "file:///etc/passwd",
          "https://user:pass@example.com",
          "http://127.1",
          "https://a\\@localhost",
          "https://example.com/%ZZ"
        }) assertThrows(IllegalArgumentException.class, () -> new UrlAnalyzer().analyze(s), s);
  }

  @Test
  void explainsHeuristicsAndSanitizesSecrets() {
    var r =
        new UrlAnalyzer()
            .analyze("http://banc0-seguranca-login.test/confirmar?token=segredo#senha");
    assertTrue(r.score() >= 30);
    assertFalse(r.url().contains("segredo"));
    assertEquals("MEDIUM", r.risk());
    assertFalse(r.indicators().isEmpty());
  }

  @Test
  void lowScoreIsNotProbability() {
    var r = new UrlAnalyzer().analyze("https://example.org");
    assertEquals(0, r.score());
    assertEquals("LOW", r.risk());
    assertEquals("rules-1.0", r.rulesVersion());
  }

  @Test
  void acceptsUnicodeButFlagsPunycode() {
    assertTrue(
        new UrlAnalyzer()
            .analyze("https://exémplo.org").indicators().stream()
                .anyMatch(i -> i.id().equals("IDN")));
  }
}
