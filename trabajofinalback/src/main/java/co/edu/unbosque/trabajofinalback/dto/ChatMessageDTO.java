package co.edu.unbosque.trabajofinalback.dto;

/**
 * Datos de un mensaje de chat. Para enviar solo se llena "contenido". Al leer,
 * el servidor devuelve todos los campos con el texto ya descifrado.
 */
public class ChatMessageDTO {

	private Long id;
	private Long chatId;
	private Long remitenteId;
	private String remitenteNombre;
	private String contenido;
	private String tipo;
	private String mediaUrl;
	private String fechaEnvio;

	public ChatMessageDTO() {
	}

	public ChatMessageDTO(String contenido) {
		this.contenido = contenido;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getChatId() {
		return chatId;
	}

	public void setChatId(Long chatId) {
		this.chatId = chatId;
	}

	public Long getRemitenteId() {
		return remitenteId;
	}

	public void setRemitenteId(Long remitenteId) {
		this.remitenteId = remitenteId;
	}

	public String getRemitenteNombre() {
		return remitenteNombre;
	}

	public void setRemitenteNombre(String remitenteNombre) {
		this.remitenteNombre = remitenteNombre;
	}

	public String getContenido() {
		return contenido;
	}

	public void setContenido(String contenido) {
		this.contenido = contenido;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getMediaUrl() {
		return mediaUrl;
	}

	public void setMediaUrl(String mediaUrl) {
		this.mediaUrl = mediaUrl;
	}

	public String getFechaEnvio() {
		return fechaEnvio;
	}

	public void setFechaEnvio(String fechaEnvio) {
		this.fechaEnvio = fechaEnvio;
	}
}