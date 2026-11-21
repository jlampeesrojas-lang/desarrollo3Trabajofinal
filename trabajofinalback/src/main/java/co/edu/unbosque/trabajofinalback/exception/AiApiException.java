package co.edu.unbosque.trabajofinalback.exception;

/** Se lanza cuando no se pudo consultar la API de inteligencia artificial o su respuesta no es la esperada. */
public class AiApiException extends Exception {

  private static final long serialVersionUID = 1L;

  /** Crea la excepción con el mensaje predeterminado. */
  public AiApiException() {
    super("No se pudo consultar la API de inteligencia artificial.");
  }

  /**
   * Crea la excepción con un mensaje personalizado.
   *
   * @param mensaje descripción del error
   */
  public AiApiException(String mensaje) {
    super(mensaje);
  }
}