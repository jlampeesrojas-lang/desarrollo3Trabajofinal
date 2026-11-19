package co.edu.unbosque.trabajofinalback.exception;

/**
 * Se lanza cuando no se pudo ubicar una IP (la API no responde, rechaza la IP o
 * falla).
 */
public class IpApiException extends Exception {

	private static final long serialVersionUID = 1L;

	public IpApiException() {
		super("No se pudo consultar la API externa de ubicación.");
	}

	public IpApiException(String mensaje) {
		super(mensaje);
	}
}