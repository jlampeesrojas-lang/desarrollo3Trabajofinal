package co.edu.unbosque.trabajofinalback.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Se usa en dos sentidos: como cuerpo de POST /admin/chats/{id}/messages (la
 * clave que escribe el admin) y como respuesta de GET /admin/chats/{id}/key (la
 * clave del chat). Coincide con ClaveChat del front.
 */
public class ClaveChatDTO {

	@NotBlank(message = "La clave es obligatoria")
	private String key;

	public ClaveChatDTO() {
	}

	public ClaveChatDTO(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
}
