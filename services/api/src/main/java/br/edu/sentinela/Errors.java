package br.edu.sentinela;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class Errors {
  @ExceptionHandler(ApiError.class)
  ResponseEntity<?> api(ApiError e) {
    return ResponseEntity.status(e.status).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    org.springframework.web.bind.MethodArgumentNotValidException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class
  })
  ResponseEntity<?> bad(Exception e) {
    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "message",
                e instanceof IllegalArgumentException
                    ? e.getMessage()
                    : "Verifique os campos enviados."));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> other(Exception e) {
    org.slf4j.LoggerFactory.getLogger(Errors.class)
        .error("Falha interna: {}", e.getClass().getSimpleName());
    return ResponseEntity.status(500)
        .body(Map.of("message", "Não foi possível concluir a operação."));
  }
}
