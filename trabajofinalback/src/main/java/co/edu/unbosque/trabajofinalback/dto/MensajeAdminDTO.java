package co.edu.unbosque.trabajofinalback.dto;

import java.time.LocalDateTime;

/**
 * Un mensaje ya descifrado para el panel admin.
 */
public class MensajeAdminDTO {

	private Long id;
	private String sender;
	private String type;
	private String content;
	private Long mediaId;
	private LocalDateTime createdAt;

	/** Constructor vacío. */
	public MensajeAdminDTO() {
		// TODO Auto-generated constructor stub
	}

	/** Crea un MensajeAdminDTO con sus datos. */
	public MensajeAdminDTO(Long id, String sender, String type, String content, Long mediaId, LocalDateTime createdAt) {
		this.id = id;
		this.sender = sender;
		this.type = type;
		this.content = content;
		this.mediaId = mediaId;
		this.createdAt = createdAt;
	}

	/** Devuelve id. */
	public Long getId() {
		return id;
	}

	/** Define id. */
	public void setId(Long id) {
		this.id = id;
	}

	/** Devuelve sender. */
	public String getSender() {
		return sender;
	}

	/** Define sender. */
	public void setSender(String sender) {
		this.sender = sender;
	}

	/** Devuelve type. */
	public String getType() {
		return type;
	}

	/** Define type. */
	public void setType(String type) {
		this.type = type;
	}

	/** Devuelve content. */
	public String getContent() {
		return content;
	}

	/** Define content. */
	public void setContent(String content) {
		this.content = content;
	}

	/** Devuelve media id. */
	public Long getMediaId() {
		return mediaId;
	}

	/** Define media id. */
	public void setMediaId(Long mediaId) {
		this.mediaId = mediaId;
	}

	/** Devuelve created at. */
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	/** Define created at. */
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	/** Devuelve el objeto como texto. */
	@Override
	public String toString() {
		return "MensajeAdminDTO [id=" + id + ", sender=" + sender + ", type=" + type + ", content=" + content
				+ ", mediaId=" + mediaId + ", createdAt=" + createdAt + "]";
	}

}