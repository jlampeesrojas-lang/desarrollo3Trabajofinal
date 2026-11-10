package co.edu.unbosque.trabajofinalback.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Imagen o audio subido a un chat. El archivo se guarda en la base de datos CIFRADO con la llave
 * del chat (así no depende del disco del servidor en el despliegue).
 */
@Entity
@Table(name = "media")
public class Media {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "chat_id", nullable = false)
  private Chat chat;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "uploader_id", nullable = false)
  private User uploader;

  /** Tipo MIME detectado por el contenido (no el que dice el cliente). */
  @Column(name = "mime_type", nullable = false, length = 50)
  private String mimeType;

  /** Tamaño original en bytes. */
  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  /** Bytes cifrados: iv de 12 bytes + cifrado + tag GCM. */
  @Lob
  @Column(name = "data_enc", nullable = false, columnDefinition = "LONGBLOB")
  private byte[] dataEnc;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public Media() {}

  public Media(Chat chat, User uploader, String mimeType, long sizeBytes, byte[] dataEnc) {
    this.chat = chat;
    this.uploader = uploader;
    this.mimeType = mimeType;
    this.sizeBytes = sizeBytes;
    this.dataEnc = dataEnc;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  public Long getId() { return id; }
  public Chat getChat() { return chat; }
  public User getUploader() { return uploader; }
  public String getMimeType() { return mimeType; }
  public long getSizeBytes() { return sizeBytes; }
  public byte[] getDataEnc() { return dataEnc; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}