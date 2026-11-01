package co.edu.unbosque.trabajofinalback.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Convierte las excepciones en respuestas JSON uniformes con la forma {"error": "...", "mensaje":
 * "..."} (la misma que documenta OpenApiConfig).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private ResponseEntity<Map<String, String>> build(HttpStatus status, String error, String message) {
    Map<String, String> body = new LinkedHashMap<>();
    body.put("error", error);
    body.put("mensaje", message);
    return ResponseEntity.status(status).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
    String detail =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return build(HttpStatus.BAD_REQUEST, "Datos inválidos", detail);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException ex) {
    return build(HttpStatus.BAD_REQUEST, "Solicitud inválida", "El cuerpo de la solicitud no es un JSON válido");
  }

  @ExceptionHandler(PasswordNotValidException.class)
  public ResponseEntity<Map<String, String>> handlePassword(PasswordNotValidException ex) {
    return build(HttpStatus.BAD_REQUEST, "Contraseña no válida", ex.getMessage());
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<Map<String, String>> handleConflict(UserAlreadyExistsException ex) {
    return build(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
  }

  @ExceptionHandler(InvalidTokenException.class)
  public ResponseEntity<Map<String, String>> handleInvalidToken(InvalidTokenException ex) {
    return build(ex.getStatus(), "Token inválido", ex.getMessage());
  }

  @ExceptionHandler(EmailNotVerifiedException.class)
  public ResponseEntity<Map<String, String>> handleNotVerified(EmailNotVerifiedException ex) {
    return build(HttpStatus.FORBIDDEN, "Correo sin confirmar", ex.getMessage());
  }

  /** Cuenta deshabilitada, bloqueada o vencida. */
  @ExceptionHandler(AccountStatusException.class)
  public ResponseEntity<Map<String, String>> handleAccountStatus(AccountStatusException ex) {
    return build(HttpStatus.FORBIDDEN, "Cuenta no disponible", "La cuenta está deshabilitada o bloqueada");
  }

  /** Credenciales incorrectas: mensaje genérico para no revelar si el usuario existe. */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException ex) {
    log.debug("Autenticación fallida: {}", ex.getMessage());
    return build(HttpStatus.UNAUTHORIZED, "No autorizado", "Usuario o contraseña incorrectos");
  }
}
