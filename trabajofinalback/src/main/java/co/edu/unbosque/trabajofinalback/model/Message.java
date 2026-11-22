package co.edu.unbosque.trabajofinalback.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Mensaje de un chat. El texto se guarda SOLO cifrado con AESUtil (AES/GCM) y
 * la llave del chat. En MySQL la columna content_enc no es legible. Un mensaje
 * eliminado no se borra: se marca con deleted y conserva su contenido cifrado.
 */
@Entity
@Table(name = "messages", indexes = @Index(name = "idx_messages_chat", columnList = "chat_id, id"))
public class Message {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "chat_id", nullable = false)
	private Chat chat;

	/** Remitente (null para mensajes de la IA). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender_id")
	private User sender;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private MessageType type;

	/** Texto cifrado con AESUtil y la llave del chat (Base64). */
	@Column(name = "content_enc", nullable = false, columnDefinition = "TEXT")
	private String contentEnc;

	/** Archivo adjunto (tabla media) para mensajes IMAGEN o AUDIO. */
	@Column(name = "media_id")
	private Long mediaId;
	
	/** Texto que acompaña a un archivo, cifrado con la llave del chat. */
	@Column(name = "caption_enc", columnDefinition = "TEXT")
	private String captionEnc;

	/** Indica si el remitente eliminó el mensaje. */
	@Column(name = "deleted", nullable = false, columnDefinition = "boolean default false")
	private boolean deleted = false;

	/** Fecha en la que se eliminó el mensaje (null si no se ha eliminado). */
	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** Constructor vacío. */
	public Message() {
	}

	/** Crea un Message con sus datos. */
	public Message(Chat chat, User sender, MessageType type, String contentEnc, Long mediaId) {
		this.chat = chat;
		this.sender = sender;
		this.type = type;
		this.contentEnc = contentEnc;
		this.mediaId = mediaId;
	}

	/** Asigna la fecha de creación justo antes de guardar. */
	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}

	public Long getId() {
		return id;
	}

	/** Define el identificador (lo asigna la base de datos al guardar). */
	public void setId(Long id) {
		this.id = id;
	}

	public Chat getChat() {
		return chat;
	}

	public User getSender() {
		return sender;
	}

	public MessageType getType() {
		return type;
	}

	public String getContentEnc() {
		return contentEnc;
	}

	public Long getMediaId() {
		return mediaId;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public String getCaptionEnc() {
		return captionEnc;
	}

	public void setCaptionEnc(String captionEnc) {
		this.captionEnc = captionEnc;
	}

	public boolean isDeleted() {
		return deleted;
	}

	/** Marca o desmarca el mensaje como eliminado. */
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public LocalDateTime getDeletedAt() {
		return deletedAt;
	}

	/** Define la fecha en la que se eliminó el mensaje. */
	public void setDeletedAt(LocalDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}
}