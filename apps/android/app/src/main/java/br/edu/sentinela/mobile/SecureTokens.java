package br.edu.sentinela.mobile;

import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

public final class SecureTokens {
  private final android.content.SharedPreferences prefs;
  private static final String ALIAS = "sentinela_session_v1";

  SecureTokens(Context c) {
    prefs = c.getSharedPreferences("secure_session", Context.MODE_PRIVATE);
  }

  private SecretKey key() throws Exception {
    KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
    ks.load(null);
    if (!ks.containsAlias(ALIAS)) {
      KeyGenerator g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
      g.init(
          new KeyGenParameterSpec.Builder(
                  ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
              .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
              .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
              .setRandomizedEncryptionRequired(true)
              .build());
      g.generateKey();
    }
    return (SecretKey) ks.getKey(ALIAS, null);
  }

  private String encrypt(String token) throws Exception {
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(Cipher.ENCRYPT_MODE, key());
    String value =
        Base64.encodeToString(c.getIV(), Base64.NO_WRAP)
            + "."
            + Base64.encodeToString(
                c.doFinal(token.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
    return value;
  }

  synchronized void save(String access, String refresh) throws Exception {
    String a=encrypt(access), r=encrypt(refresh);
    if(!prefs.edit().putString("access",a).putString("refresh",r).commit()) throw new IllegalStateException("Falha ao armazenar sessão.");
  }

  synchronized String get(String name) {
    try {
      String value = prefs.getString(name, "");
      if (value.isEmpty()) return "";
      String[] a = value.split("\\.");
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(
          Cipher.DECRYPT_MODE,
          key(),
          new GCMParameterSpec(128, Base64.decode(a[0], Base64.NO_WRAP)));
      return new String(c.doFinal(Base64.decode(a[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
    } catch (Exception e) {
      clear();
      return "";
    }
  }

  synchronized void clear() {
    prefs.edit().clear().commit();
  }
}
