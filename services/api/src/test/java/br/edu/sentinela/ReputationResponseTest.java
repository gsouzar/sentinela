package br.edu.sentinela;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ReputationResponseTest {
  final ObjectMapper json = new ObjectMapper();

  @Test
  void emptyMatchesAreNotAThreat() throws Exception {
    assertFalse(ReputationService.hasGoogleMatches(json.readTree("{}")));
    assertFalse(ReputationService.hasGoogleMatches(json.readTree("{\"matches\":[]}")));
    assertTrue(
        ReputationService.hasGoogleMatches(
            json.readTree("{\"matches\":[{\"threatType\":\"SOCIAL_ENGINEERING\"}]}")));
  }

  @Test
  void malformedResponseIsNotASafeResult() throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> ReputationService.hasGoogleMatches(json.readTree("{\"matches\":\"invalid\"}")));
    assertThrows(
        IllegalArgumentException.class,
        () -> ReputationService.hasGoogleMatches(json.readTree("[]")));
  }
}
