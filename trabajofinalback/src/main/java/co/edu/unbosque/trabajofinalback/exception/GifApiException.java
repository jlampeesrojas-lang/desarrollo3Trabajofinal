package co.edu.unbosque.trabajofinalback.exception;

/**
 * Se lanza cuando no se pudo consultar la API de GIPHY o su respuesta no es la
 * esperada.
 */
public class GifApiException extends Exception {

	private static final long serialVersionUID = 1L;

	/** Crea la excepción con el mensaje predeterminado de error al consultar la API de GIFs. */
	public GifApiException() {
		super("No se pudo consultar la API externa de GIFs.");
	}

	/**
	 * Crea la excepción con un mensaje personalizado.
	 *
	 * @param mensaje descripción del error al consultar la API de GIFs
	 */
	public GifApiException(String mensaje) {
		super(mensaje);
	}
}