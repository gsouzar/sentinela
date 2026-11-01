package br.edu.sentinela;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionFilter extends OncePerRequestFilter {
  final Store store;
  @org.springframework.beans.factory.annotation.Value("${sentinela.require-mfa:false}") boolean requireMfa;

  public SessionFilter(Store s) {
    store = s;
  }

  public record Viewer(
      String id, String name, String email, String role, String sessionId, boolean mfa) {}

  protected void doFilterInternal(
      HttpServletRequest r, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String token = null;
    String auth = r.getHeader("Authorization");
    if (auth != null && auth.startsWith("Bearer ")) token = auth.substring(7);
    else if (r.getCookies() != null)
      for (var c : r.getCookies())
        if (c.getName().equals("SENTINELA_SESSION")) token = c.getValue();
    if (token != null && token.length() < 200) {
      var row =
          store.one(
              "SELECT u.id,u.name,u.email,u.role,u.mfa_enabled,s.id AS session_id FROM sessions s"
                  + " JOIN app_users u ON u.id=s.user_id WHERE s.token_hash=? AND s.expires_at>?"
                  + " AND u.enabled=TRUE",
              Crypto.hash(token),
              System.currentTimeMillis());
      if (row != null) {
        var v =
            new Viewer(
                row.get("id").toString(),
                row.get("name").toString(),
                row.get("email").toString(),
                row.get("role").toString(),
                row.get("session_id").toString(),
                Boolean.TRUE.equals(row.get("mfa_enabled")));
        String path=r.getRequestURI();
        if(!v.mfa() && (requireMfa || "ADMIN".equals(v.role())) && !Set.of("/api/v1/me","/api/v1/me/mfa/setup","/api/v1/me/mfa/enable","/api/v1/me/password","/api/v1/me/sessions","/api/v1/me/sessions/revoke-all","/api/v1/auth/logout","/api/v1/auth/login","/api/v1/auth/refresh","/api/v1/csrf").contains(path) && !path.startsWith("/api/v1/me/sessions/")) {
          response.setStatus(428); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8"); response.getWriter().write("{\"code\":\"MFA_ENROLLMENT_REQUIRED\",\"message\":\"Ative o segundo fator em Segurança para continuar.\"}"); return;
        }
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    v, null, List.of(new SimpleGrantedAuthority("ROLE_" + v.role()))));
      }
    }
    chain.doFilter(r, response);
  }
}
