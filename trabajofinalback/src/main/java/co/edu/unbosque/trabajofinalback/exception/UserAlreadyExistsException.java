package co.edu.unbosque.trabajofinalback.exception;

/** Se lanza cuando el nombre de usuario o el correo ya están registrados. */
public class UserAlreadyExistsException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public UserAlreadyExistsException(String message) {
    super(message);
  }
}
