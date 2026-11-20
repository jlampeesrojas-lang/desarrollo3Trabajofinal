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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Registro de auditoría: ingresos, fecha, IP, ubicación y metadatos de la actividad del usuario.
 * No tiene llave foránea a users a propósito, para que el historial sobreviva si se borra el
 * usuario.
 */
@Entity
@Table(
    name = "audit_log",
    indexes = {
/** AuditLog. */
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
  @Column(name="action",nullable = false, length = 40)
  @JdbcTypeCode(SqlTypes.VARCHAR)
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

  /** Constructor vacío. */
  public AuditLog() {}

  /** Crea un AuditLog con sus datos. */
  public AuditLog(AuditAction action, Long userId, String ip, String location, String detail) {
    this.action = action;
    this.userId = userId;
    this.ip = ip;
    this.location = location;
    this.detail = detail;
  }

  /** On create. */
  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  /** Devuelve id. */
  public Long getId() {
    return id;
  }

  /** Devuelve user id. */
  public Long getUserId() {
    return userId;
  }

  /** Define user id. */
  public void setUserId(Long userId) {
    this.userId = userId;
  }

  /** Devuelve action. */
  public AuditAction getAction() {
    return action;
  }

  /** Define action. */
  public void setAction(AuditAction action) {
    this.action = action;
  }

  /** Devuelve ip. */
  public String getIp() {
    return ip;
  }

  /** Define ip. */
  public void setIp(String ip) {
    this.ip = ip;
  }

  /** Devuelve location. */
  public String getLocation() {
    return location;
  }

  /** Define location. */
  public void setLocation(String location) {
    this.location = location;
  }

  /** Devuelve detail. */
  public String getDetail() {
    return detail;
  }

  /** Define detail. */
  public void setDetail(String detail) {
    this.detail = detail;
  }

  /** Devuelve created at. */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}