package co.edu.unbosque.trabajofinalback.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Token de un solo uso enviado por correo (confirmación de correo o recuperación de contraseña).
 * En la base de datos solo se guarda el HASH SHA-256 del token, nunca el token original.
 */
@Entity
@Table(name = "email_tokens")
public class EmailToken {

  /** Para qué sirve el token. */
  public enum TokenType {
    CONFIRM_EMAIL,
    RESET_PASSWORD
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Usuario dueño del token. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** Hash SHA-256 (hex) del token enviado por correo. */
  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TokenType type;

  /** Fecha y hora en que el token deja de ser válido. */
  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  /** Fecha y hora en que se usó (null si aún no se ha usado). */
  @Column(name = "used_at")
  private LocalDateTime usedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public EmailToken() {}

  public EmailToken(User user, String tokenHash, TokenType type, LocalDateTime expiresAt) {
    this.user = user;
    this.tokenHash = tokenHash;
    this.type = type;
    this.expiresAt = expiresAt;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  /** @return true si el token ya venció */
  public boolean isExpired() {
    return expiresAt.isBefore(LocalDateTime.now());
  }

  /** @return true si el token ya fue usado */
  public boolean isUsed() {
    return usedAt != null;
  }

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public TokenType getType() {
    return type;
  }

  public void setType(TokenType type) {
    this.type = type;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public LocalDateTime getUsedAt() {
    return usedAt;
  }

  public void setUsedAt(LocalDateTime usedAt) {
    this.usedAt = usedAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
