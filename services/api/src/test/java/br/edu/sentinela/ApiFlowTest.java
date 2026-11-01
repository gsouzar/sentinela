package br.edu.sentinela;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
      "sentinela.demo=true", "sentinela.require-mfa=false",
      "sentinela.encryption-key=test-only-encryption-key-not-for-production"
    })
@AutoConfigureMockMvc
@org.springframework.test.context.TestExecutionListeners(
    listeners = {
      org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,
      org.springframework.test.context.web.ServletTestExecutionListener.class
    },
    mergeMode = org.springframework.test.context.TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
class ApiFlowTest {
  @Autowired Store store;

  @BeforeEach
  void resetLimits() {
    store.jdbc().update("DELETE FROM rate_buckets");
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  String token(String email) throws Exception {
    var s =
        mvc.perform(
                post("/api/v1/auth/register")
                    .header("X-Sentinela-Client", "android")
                    .contentType("application/json")
                    .content(
                        "{\"name\":\"Pessoa Teste\",\"email\":\""
                            + email
                            + "\",\"password\":\"UmaSenhaForte#123\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(s).get("accessToken").asText();
  }

  @Test
  void persistsResultsAndRejectsCrossAccountAccess() throws Exception {
    String a = token("a" + System.nanoTime() + "@example.org"),
        b = token("b" + System.nanoTime() + "@example.org");
    String s =
        mvc.perform(
                post("/api/v1/analyses")
                    .header("Authorization", "Bearer " + a)
                    .contentType("application/json")
                    .content(
                        "{\"url\":\"http://banc0-seguranca-login.test/confirmar?token=segredo\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = json.readTree(s).get("id").asText();
    mvc.perform(get("/api/v1/analyses/" + id).header("Authorization", "Bearer " + b))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/analyses/" + id).header("Authorization", "Bearer " + a))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.score").value(80));
    mvc.perform(get("/api/v1/admin/audit").header("Authorization", "Bearer " + a))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/chat")
                .header("Authorization", "Bearer " + a)
                .contentType("application/json")
                .content("{\"message\":\"Por que?\",\"analysisId\":\"" + id + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mode").value("LOCAL"));
    mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + a))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + a))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void anonymousAndCsrfAreDenied() throws Exception {
    mvc.perform(get("/api/v1/analyses")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void validatesInputAndDoesNotReflectHtml() throws Exception {
    String t = token("c" + System.nanoTime() + "@example.org");
    mvc.perform(
            post("/api/v1/analyses")
                .header("Authorization", "Bearer " + t)
                .contentType("application/json")
                .content("{\"url\":\"javascript:alert(1)\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/analyses")
                .header("Authorization", "Bearer " + t)
                .param("risk", "' OR 1=1 --"))
        .andExpect(status().isBadRequest());
  }

  @Autowired Crypto crypto;

  @Test
  void rotatesRefreshAndRevokesOnReplay() throws Exception {
    String email = "refresh" + System.nanoTime() + "@example.org";
    token(email);
    String b =
        mvc.perform(
                post("/api/v1/auth/login")
                    .header("X-Sentinela-Client", "android")
                    .contentType("application/json")
                    .content("{\"email\":\"" + email + "\",\"password\":\"UmaSenhaForte#123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String refresh = json.readTree(b).get("refreshToken").asText();
    String r =
        mvc.perform(
                post("/api/v1/auth/refresh")
                    .header("X-Sentinela-Client", "android")
                    .contentType("application/json")
                    .content("{\"refreshToken\":\"" + refresh + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String current = json.readTree(r).get("accessToken").asText();
    mvc.perform(
            post("/api/v1/auth/refresh")
                .header("X-Sentinela-Client", "android")
                .contentType("application/json")
                .content("{\"refreshToken\":\"" + refresh + "\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + current))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void mfaRecoveryCodeWorksOnlyOnce() throws Exception {
    String email = "mfa" + System.nanoTime() + "@example.org", t = token(email);
    String setup =
        mvc.perform(
                post("/api/v1/me/mfa/setup")
                    .header("Authorization", "Bearer " + t)
                    .contentType("application/json")
                    .content("{\"password\":\"UmaSenhaForte#123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String secret = json.readTree(setup).get("secret").asText();
    String code = crypto.totp(secret, System.currentTimeMillis() / 30000);
    String enabled =
        mvc.perform(
                post("/api/v1/me/mfa/enable")
                    .header("Authorization", "Bearer " + t)
                    .contentType("application/json")
                    .content("{\"password\":\"UmaSenhaForte#123\",\"code\":\"" + code + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String recovery = json.readTree(enabled).get("recoveryCodes").get(0).asText();
    String login =
        "{\"email\":\""
            + email
            + "\",\"password\":\"UmaSenhaForte#123\",\"code\":\""
            + recovery
            + "\"}";
    mvc.perform(
            post("/api/v1/auth/login")
                .header("X-Sentinela-Client", "android")
                .contentType("application/json")
                .content(login))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/auth/login")
                .header("X-Sentinela-Client", "android")
                .contentType("application/json")
                .content(login))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void limitsJsonBody() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .header("X-Sentinela-Client", "android")
                .contentType("application/json")
                .content("x".repeat(17000)))
        .andExpect(status().isPayloadTooLarge());
  }

  @Test
  void actualCookieSessionRequiresCsrf() throws Exception {
    String email = "cookie" + System.nanoTime() + "@example.org";
    var r =
        mvc.perform(
                post("/api/v1/auth/register")
                    .with(
                        org.springframework.security.test.web.servlet.request
                            .SecurityMockMvcRequestPostProcessors.csrf()
                            .asHeader())
                    .contentType("application/json")
                    .content(
                        "{\"name\":\"Pessoa\",\"email\":\""
                            + email
                            + "\",\"password\":\"UmaSenhaForte#123\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    org.junit.jupiter.api.Assertions.assertFalse(r.getContentAsString().contains("accessToken"));
    String header = r.getHeader("Set-Cookie"),
        session = header.substring(header.indexOf('=') + 1, header.indexOf(';'));
    var cookie = new jakarta.servlet.http.Cookie("SENTINELA_SESSION", session);
    mvc.perform(get("/api/v1/me").cookie(cookie)).andExpect(status().isOk());
    mvc.perform(post("/api/v1/auth/logout").cookie(cookie)).andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/auth/logout")
                .cookie(cookie)
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.csrf()
                        .asHeader()))
        .andExpect(status().isOk());
  }

  @Test
  void throttlesRepeatedFailedCredentials() throws Exception {
    String email = "missing" + System.nanoTime() + "@example.org";
    String body =
        json.writeValueAsString(java.util.Map.of("email", email, "password", "incorrectPassword"));
    for (int i = 0; i < 10; i++)
      mvc.perform(
              post("/api/v1/auth/login")
                  .header("X-Sentinela-Client", "android")
                  .with(
                      r -> {
                        r.setRemoteAddr("192.0.2.20");
                        return r;
                      })
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/v1/auth/login")
                .header("X-Sentinela-Client", "android")
                .with(
                    r -> {
                      r.setRemoteAddr("192.0.2.20");
                      return r;
                    })
                .contentType("application/json")
                .content(body))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void wrongStepUpDoesNotEndAuthenticatedSession() throws Exception {
    String t = token("proof" + System.nanoTime() + "@example.org");
    mvc.perform(
            post("/api/v1/me/mfa/setup")
                .header("Authorization", "Bearer " + t)
                .contentType("application/json")
                .content(
                    json.writeValueAsString(java.util.Map.of("password", "incorrect-password"))))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + t))
        .andExpect(status().isOk());
  }
}
