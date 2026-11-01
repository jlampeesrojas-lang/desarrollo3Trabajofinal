package co.edu.unbosque.trabajofinalback.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando un token (de correo o refresh) es inválido, está vencido o ya fue usado. Lleva el
 * código HTTP con el que debe responder la API (400 para correo, 401 para refresh).
 */
public class InvalidTokenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final HttpStatus status;

  public InvalidTokenException(String message, HttpStatus status) {
    super(message);
    this.status = status;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
