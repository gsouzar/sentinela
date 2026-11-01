package br.edu.sentinela;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class BodyLimitFilter extends OncePerRequestFilter {
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    if (!java.util.List.of("POST", "PUT", "PATCH").contains(req.getMethod())) {
      chain.doFilter(req, res);
      return;
    }
    byte[] bytes = req.getInputStream().readNBytes(16385);
    if (bytes.length > 16384) {
      res.setStatus(413);
      res.setContentType("application/json");
      res.setCharacterEncoding("UTF-8");
      res.getWriter().write("{\"message\":\"O corpo da requisição excede 16 KB.\"}");
      return;
    }
    var wrapped =
        new HttpServletRequestWrapper(req) {
          public ServletInputStream getInputStream() {
            var in = new ByteArrayInputStream(bytes);
            return new ServletInputStream() {
              public int read() {
                return in.read();
              }

              public boolean isFinished() {
                return in.available() == 0;
              }

              public boolean isReady() {
                return true;
              }

              public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException();
              }
            };
          }

          public BufferedReader getReader() {
            return new BufferedReader(
                new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
          }
        };
    chain.doFilter(wrapped, res);
  }
}
