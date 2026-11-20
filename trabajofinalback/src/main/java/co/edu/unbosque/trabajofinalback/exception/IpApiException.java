package co.edu.unbosque.trabajofinalback.exception;

/**
 * Se lanza cuando no se pudo ubicar una IP (la API no responde, rechaza la IP o
 * falla).
 */
public class IpApiException extends Exception {

	private static final long serialVersionUID = 1L;

	/** Crea la excepción con el mensaje predeterminado de error al consultar la API de ubicación. */
	public IpApiException() {
	    super("No se pudo consultar la API externa de ubicación.");
	}

	/**
	 * Crea la excepción con un mensaje personalizado.
	 *
	 * @param mensaje descripción del error al consultar la ubicación de una IP
	 */
	public IpApiException(String mensaje) {
		super(mensaje);
	}
}