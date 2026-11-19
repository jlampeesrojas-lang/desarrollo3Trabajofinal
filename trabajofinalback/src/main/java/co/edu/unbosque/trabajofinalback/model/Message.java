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
 * Mensaje de un chat. El texto se guarda SOLO cifrado con AESUtil (AES/GCM) y la llave del chat.
 * En MySQL la columna content_enc no es legible.
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

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public Message() {}

  public Message(Chat chat, User sender, MessageType type, String contentEnc, Long mediaId) {
    this.chat = chat;
    this.sender = sender;
    this.type = type;
    this.contentEnc = contentEnc;
    this.mediaId = mediaId;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  public Long getId() { return id; }
  public Chat getChat() { return chat; }
  public User getSender() { return sender; }
  public MessageType getType() { return type; }
  public String getContentEnc() { return contentEnc; }
  public Long getMediaId() { return mediaId; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}