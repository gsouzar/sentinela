package br.edu.sentinela;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class Store {
  final JdbcTemplate db;
  final ObjectMapper json;

  public Store(JdbcTemplate db, ObjectMapper json) {
    this.db = db;
    this.json = json;
  }

  public JdbcTemplate jdbc() {
    return db;
  }

  public Map<String, Object> one(String sql, Object... args) {
    var rows = db.queryForList(sql, args);
    return rows.isEmpty() ? null : lower(rows.getFirst());
  }

  public List<Map<String, Object>> rows(String sql, Object... args) {
    return db.queryForList(sql, args).stream().map(this::lower).toList();
  }

  Map<String, Object> lower(Map<String, Object> m) {
    Map<String, Object> r = new LinkedHashMap<>();
    m.forEach((k, v) -> r.put(k.toLowerCase(Locale.ROOT), v));
    return r;
  }

  public String encode(Object o) {
    try {
      return json.writeValueAsString(o);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public Map<String, Object> decode(String s) {
    try {
      return json.readValue(
          s, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public void audit(String user, String action, String detail) {
    db.update(
        "INSERT INTO audit_events VALUES(?,?,?,?,?)",
        UUID.randomUUID().toString(),
        user,
        action,
        detail,
        System.currentTimeMillis());
  }

  @org.springframework.transaction.annotation.Transactional(
      propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public void failedAuthentication(String user, String detail) {
    audit(user, "LOGIN_FAILED", detail);
  }

  @org.springframework.transaction.annotation.Transactional(
      propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public void limit(String key, int max, int seconds) {
    long now = System.currentTimeMillis();
    db.update(
        "DELETE FROM rate_buckets WHERE bucket_key=? AND window_start < ?",
        key,
        now - seconds * 1000L);
    db.update("INSERT INTO rate_buckets VALUES(?,0,?) ON CONFLICT DO NOTHING", key, now);
    int n =
        db.update("UPDATE rate_buckets SET hits=hits+1 WHERE bucket_key=? AND hits < ?", key, max);
    if (n == 0) throw new ApiError(429, "Muitas tentativas. Aguarde e tente novamente.");
  }
}
