package br.edu.sentinela;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Bootstrap implements CommandLineRunner {
  final Store s;
  final PasswordEncoder p;
  final String email, password;

  Bootstrap(
      Store s,
      PasswordEncoder p,
      @Value("${sentinela.admin-email}") String e,
      @Value("${sentinela.admin-password}") String x) {
    this.s = s;
    this.p = p;
    email = e;
    password = x;
  }

  public void run(String... args) {
    if (email.isBlank()) return;
    if (password.length() < 16)
      throw new IllegalStateException("ADMIN_PASSWORD deve ter pelo menos 16 caracteres.");
    if (s.one("SELECT id FROM app_users WHERE email=?", email.toLowerCase(Locale.ROOT)) == null) {
      String id = UUID.randomUUID().toString();
      s.jdbc()
          .update(
              "INSERT INTO app_users(id,name,email,password_hash,role,created_at)"
                  + " VALUES(?,?,?,?,?,?)",
              id,
              "Administrador",
              email.toLowerCase(Locale.ROOT),
              p.encode(password),
              "ADMIN",
              System.currentTimeMillis());
      s.audit(id, "ADMIN_BOOTSTRAP", "Administrador inicial criado");
    }
  }
}
