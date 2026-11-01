package br.edu.sentinela;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
  @Bean
  org.springframework.security.crypto.password.PasswordEncoder passwords() {
    return new org.springframework.security.crypto.argon2.Argon2PasswordEncoder(16,32,1,19456,2);
  }

  @Bean
  org.springframework.boot.web.servlet.FilterRegistrationBean<SessionFilter> sessionRegistration(SessionFilter f) { var b=new org.springframework.boot.web.servlet.FilterRegistrationBean<>(f); b.setEnabled(false); return b; }

  @Bean
  SecurityFilterChain security(
      HttpSecurity h,
      SessionFilter filter,
      @Value("${sentinela.allowed-origin}") String origin,
      @Value("${sentinela.secure-cookies}") boolean secure)
      throws Exception {
    var repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repo.setCookieCustomizer(b -> b.path("/").sameSite("Strict").secure(secure));
    h.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(
            c ->
                c.csrfTokenRepository(repo)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers(
                        r -> {
                          if(r.getCookies()!=null) for(var cookie:r.getCookies()) if("SENTINELA_SESSION".equals(cookie.getName())) return false;
                          String a = r.getHeader("Authorization");
                          return r.getRequestURI().equals("/api/v1/ingest/events") || r.getRequestURI().startsWith("/api/v1/integrations/telegram/") || a != null && a.startsWith("Bearer ")
                              || "android".equals(r.getHeader("X-Sentinela-Client"))
                                  && r.getRequestURI().startsWith("/api/v1/auth/");
                        }))
        .cors(
            c ->
                c.configurationSource(
                    r -> {
                      var x = new CorsConfiguration();
                      x.setAllowedOrigins(List.of(origin));
                      x.setAllowedMethods(List.of("GET", "POST", "DELETE", "PATCH", "OPTIONS"));
                      x.setAllowedHeaders(
                          List.of(
                              "Content-Type",
                              "Authorization",
                              "X-XSRF-TOKEN",
                              "X-Sentinela-Client"));
                      x.setAllowCredentials(true);
                      return x;
                    }))
        .headers(
            x ->
                x.contentSecurityPolicy(
                    c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/v1/health",
                        "/api/v1/csrf",
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh", "/api/v1/ingest/events", "/api/v1/integrations/telegram/**")
                    .permitAll()
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (r, s, x) -> {
                          s.setStatus(401);
                          s.setContentType("application/json");
                          s.setCharacterEncoding("UTF-8");
                          s.getWriter()
                              .write("{\"message\":\"Entre na sua conta para continuar.\"}");
                        })
                    .accessDeniedHandler(
                        (r, s, x) -> {
                          s.setStatus(403);
                          s.setContentType("application/json");
                          s.setCharacterEncoding("UTF-8");
                          s.getWriter()
                              .write(
                                  "{\"message\":\"Acesso negado ou token de formulário"
                                      + " inválido.\"}");
                        }))
        .addFilterBefore(filter, AnonymousAuthenticationFilter.class);
    return h.build();
  }
}
