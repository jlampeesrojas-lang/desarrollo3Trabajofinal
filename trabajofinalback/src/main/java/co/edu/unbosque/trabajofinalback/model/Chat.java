package co.edu.unbosque.trabajofinalback.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Conversación. Cada chat tiene un código único visible (por ejemplo CH-7KQ2MZ) y su propia llave
 * AES de 16 caracteres, que se guarda cifrada con AESUtil.
 */
@Entity
@Table(name = "chats")
public class Chat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  
  @Column(nullable = false, unique = true, length = 12)
  private String code;

  /** Llave AES del chat (16 caracteres) cifrada con AESUtil.encrypt. */
  @Column(name = "key_enc", nullable = false, length = 120)
  private String keyEnc;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  
  @Column(name = "last_message_at")
  private LocalDateTime lastMessageAt;

  public Chat() {}

  public Chat(String code, String keyEnc) {
    this.code = code;
    this.keyEnc = keyEnc;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  public Long getId() { return id; }
  public String getCode() { return code; }
  public String getKeyEnc() { return keyEnc; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getLastMessageAt() { return lastMessageAt; }
  public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
}