package co.edu.unbosque.trabajofinalback.exception;

/** Se lanza cuando el usuario intenta iniciar sesión sin haber confirmado su correo. */
public class EmailNotVerifiedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public EmailNotVerifiedException() {
    super("Debes confirmar tu correo antes de iniciar sesión");
  }
}
