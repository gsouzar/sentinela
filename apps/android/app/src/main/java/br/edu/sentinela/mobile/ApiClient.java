package br.edu.sentinela.mobile;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

public final class ApiClient {
  private final SecureTokens tokens;

  ApiClient(SecureTokens t) {
    tokens = t;
  }

  public static class Failure extends Exception {
    final int status;

    Failure(int s, String m) {
      super(m);
      status = s;
    }
  }

  synchronized JSONObject call(String path, String method, JSONObject body) throws Exception {
    try {
      return request(path, method, body, true);
    } catch (Failure e) {
      if (e.status != 401 || path.startsWith("/auth/")) throw e;
      String refresh = tokens.get("refresh");
      if (refresh.isEmpty()) {
        tokens.clear();
        throw e;
      }
      try {
        JSONObject r =
            request("/auth/refresh", "POST", new JSONObject().put("refreshToken", refresh), false);
        save(r);
      } catch (Exception x) {
        tokens.clear();
        throw new Failure(401, "Sua sessão expirou. Entre novamente.");
      }
      return request(path, method, body, true);
    }
  }

  void save(JSONObject r) throws Exception {
    tokens.save(r.getString("accessToken"), r.getString("refreshToken"));
  }

  private JSONObject request(String path, String method, JSONObject body, boolean auth)
      throws Exception {
    if (!path.startsWith("/") || path.contains("://")) throw new IllegalArgumentException();
    URL url = new URL(BuildConfig.API_BASE + path);
    HttpURLConnection c = (HttpURLConnection) url.openConnection();
    c.setInstanceFollowRedirects(false);
    c.setConnectTimeout(10000);
    c.setReadTimeout(25000);
    c.setRequestMethod(method);
    c.setRequestProperty("Content-Type", "application/json");
    c.setRequestProperty("X-Sentinela-Client", "android");
    if (auth && !tokens.get("access").isEmpty())
      c.setRequestProperty("Authorization", "Bearer " + tokens.get("access"));
    try {
      if (body != null) {
        byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
        c.setDoOutput(true);
        c.setFixedLengthStreamingMode(data.length);
        try (OutputStream out = c.getOutputStream()) {
          out.write(data);
        }
      }
      int status = c.getResponseCode();
      InputStream in = status < 400 ? c.getInputStream() : c.getErrorStream();
      String text = "";
      if (in != null)
        try (in;
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
          byte[] b = new byte[4096];
          int n, total = 0;
          while ((n = in.read(b)) != -1) {
            total += n;
            if (total > 1048576) throw new IOException("Resposta muito grande.");
            out.write(b, 0, n);
          }
          text = out.toString(StandardCharsets.UTF_8.name());
        }
      JSONObject response =
          text.startsWith("[")
              ? new JSONObject().put("items", new JSONArray(text))
              : new JSONObject(text.isEmpty() ? "{}" : text);
      if (status >= 400)
        throw new Failure(status, response.optString("message", "Falha ao conectar."));
      return response;
    } finally {
      c.disconnect();
    }
  }
}
