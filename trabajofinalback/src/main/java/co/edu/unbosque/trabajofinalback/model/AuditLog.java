package co.edu.unbosque.trabajofinalback.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Registro de auditoría: ingresos, fecha, IP, ubicación y metadatos de la actividad del usuario.
 * No tiene llave foránea a users a propósito, para que el historial sobreviva si se borra el
 * usuario.
 */
@Entity
@Table(
    name = "audit_log",
    indexes = {
      @Index(name = "idx_audit_user", columnList = "user_id"),
      @Index(name = "idx_audit_created", columnList = "created_at")
    })
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Usuario que generó el evento (null si es anónimo, por ejemplo un login fallido). */
  @Column(name = "user_id")
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private AuditAction action;

  /** Dirección IP de origen. */
  @Column(length = 45)
  private String ip;

  /** Ubicación aproximada (ciudad/país). Se llena en la entrega del 8 de noviembre. */
  @Column(length = 150)
  private String location;

  /** Metadatos del evento (por ejemplo, tamaño o tipo del mensaje; nunca contenido ni claves). */
  @Column(length = 500)
  private String detail;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public AuditLog() {}

  public AuditLog(AuditAction action, Long userId, String ip, String location, String detail) {
    this.action = action;
    this.userId = userId;
    this.ip = ip;
    this.location = location;
    this.detail = detail;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public AuditAction getAction() {
    return action;
  }

  public void setAction(AuditAction action) {
    this.action = action;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
