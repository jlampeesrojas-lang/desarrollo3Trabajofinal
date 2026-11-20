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

	public MensajeAdminDTO() {
		// TODO Auto-generated constructor stub
	}

	public MensajeAdminDTO(Long id, String sender, String type, String content, Long mediaId, LocalDateTime createdAt) {
		this.id = id;
		this.sender = sender;
		this.type = type;
		this.content = content;
		this.mediaId = mediaId;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Long getMediaId() {
		return mediaId;
	}

	public void setMediaId(Long mediaId) {
		this.mediaId = mediaId;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public String toString() {
		return "MensajeAdminDTO [id=" + id + ", sender=" + sender + ", type=" + type + ", content=" + content
				+ ", mediaId=" + mediaId + ", createdAt=" + createdAt + "]";
	}

}
