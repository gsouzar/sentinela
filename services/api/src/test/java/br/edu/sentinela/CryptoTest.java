package br.edu.sentinela;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CryptoTest {
  @Test
  void refusesWeakConfiguredEncryptionKey() {
    assertThrows(IllegalArgumentException.class, () -> new Crypto("weak-key"));
  }
  @Test
  void totpMatchesRfcVector() {
    var c = new Crypto("test-only-encryption-key-32-characters");
    assertEquals("287082", c.totp("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", 1));
  }

  @Test
  void encryptsWithRandomNonce() {
    var c = new Crypto("test-only-encryption-key-32-characters");
    var a = c.encrypt("segredo");
    assertNotEquals(a, c.encrypt("segredo"));
    assertEquals("segredo", c.decrypt(a));
    assertThrows(ApiError.class, () -> c.decrypt(a + "invalid"));
  }
}
