package co.edu.unbosque.trabajofinalback.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/** Participante de un chat. Guarda hasta cuándo leyó, para calcular los mensajes no leídos. */
@Entity
@Table(
    name = "chat_participants",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_chat_participants", columnNames = {"chat_id", "user_id"}))
public class ChatParticipant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "chat_id", nullable = false)
  private Chat chat;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "joined_at", nullable = false, updatable = false)
  private LocalDateTime joinedAt;

  /** Última vez que el usuario marcó el chat como leído (null = nunca). */
  @Column(name = "last_read_at")
  private LocalDateTime lastReadAt;

  public ChatParticipant() {}

  public ChatParticipant(Chat chat, User user) {
    this.chat = chat;
    this.user = user;
  }

  @PrePersist
  void onCreate() {
    if (joinedAt == null) {
      joinedAt = LocalDateTime.now();
    }
  }

  public Long getId() { return id; }
  public Chat getChat() { return chat; }
  public User getUser() { return user; }
  public LocalDateTime getJoinedAt() { return joinedAt; }
  public LocalDateTime getLastReadAt() { return lastReadAt; }
  public void setLastReadAt(LocalDateTime lastReadAt) { this.lastReadAt = lastReadAt; }
}