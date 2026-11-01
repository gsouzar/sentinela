package br.edu.sentinela;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Crypto {
  private final byte[] key;
  private final SecureRandom random = new SecureRandom();

  public Crypto(@Value("${sentinela.encryption-key}") String secret) {
    if (!secret.isBlank() && secret.length() < 32)
      throw new IllegalArgumentException("ENCRYPTION_KEY precisa de pelo menos 32 caracteres aleatórios.");
    key = secret.isBlank() ? null : HexFormat.of().parseHex(hash(secret));
  }

  public String token() {
    byte[] b = new byte[32];
    random.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  public static String hash(String s) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public String encrypt(String s) {
    if (key == null)
      throw new ApiError(503, "Configure ENCRYPTION_KEY para ativar o segundo fator.");
    try {
      byte[] iv = new byte[12];
      random.nextBytes(iv);
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
      return Base64.getEncoder().encodeToString(iv)
          + "."
          + Base64.getEncoder().encodeToString(c.doFinal(s.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public String decrypt(String s) {
    try {
      var a = s.split("\\.");
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(key, "AES"),
          new GCMParameterSpec(128, Base64.getDecoder().decode(a[0])));
      return new String(c.doFinal(Base64.getDecoder().decode(a[1])), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new ApiError(503, "Segundo fator temporariamente indisponível.");
    }
  }

  public String newTotpSecret() {
    byte[] b = new byte[20];
    random.nextBytes(b);
    StringBuilder s = new StringBuilder();
    int bits = 0, v = 0;
    for (byte n : b) {
      v = (v << 8) | (n & 255);
      bits += 8;
      while (bits >= 5) {
        s.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".charAt((v >> (bits - 5)) & 31));
        bits -= 5;
      }
    }
    return s.toString();
  }

  public long verifyTotp(String secret, String code, long last) {
    if (code == null || !code.matches("[0-9]{6}")) return -1;
    long now = System.currentTimeMillis() / 30000;
    for (long t = now - 1; t <= now + 1; t++)
      if (t > last
          && MessageDigest.isEqual(
              totp(secret, t).getBytes(StandardCharsets.US_ASCII),
              code.getBytes(StandardCharsets.US_ASCII))) return t;
    return -1;
  }

  public String totp(String secret, long counter) {
    try {
      java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
      int v = 0, bits = 0;
      for (char c : secret.toCharArray()) {
        int n = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(c);
        if (n < 0) throw new IllegalArgumentException();
        v = (v << 5) | n;
        bits += 5;
        if (bits >= 8) {
          out.write((v >> (bits - 8)) & 255);
          bits -= 8;
        }
      }
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(out.toByteArray(), "HmacSHA1"));
      byte[] h = mac.doFinal(java.nio.ByteBuffer.allocate(8).putLong(counter).array());
      int o = h[h.length - 1] & 15;
      int n =
          ((h[o] & 127) << 24)
              | ((h[o + 1] & 255) << 16)
              | ((h[o + 2] & 255) << 8)
              | (h[o + 3] & 255);
      return String.format(java.util.Locale.ROOT, "%06d", n % 1000000);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
