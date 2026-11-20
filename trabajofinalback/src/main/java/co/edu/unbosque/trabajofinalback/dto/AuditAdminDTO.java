package co.edu.unbosque.trabajofinalback.dto;

import java.time.LocalDateTime;

/**
 * Una fila de la auditoría para el panel admin.
 */
public class AuditAdminDTO {
	private Long id;
	private String username;
	private String action;
	private String ip;
	private String location;
	private String detail;
	private LocalDateTime createdAt;

	/** Constructor vacío. */
	public AuditAdminDTO() {
		// TODO Auto-generated constructor stub
	}

	/** Crea un AuditAdminDTO con sus datos. */
	public AuditAdminDTO(Long id, String username, String action, String ip, String location, String detail,
			LocalDateTime createdAt) {
		this.id = id;
		this.username = username;
		this.action = action;
		this.ip = ip;
		this.location = location;
		this.detail = detail;
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

	/** Devuelve username. */
	public String getUsername() {
		return username;
	}

	/** Define username. */
	public void setUsername(String username) {
		this.username = username;
	}

	/** Devuelve action. */
	public String getAction() {
		return action;
	}

	/** Define action. */
	public void setAction(String action) {
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

	/** Define created at. */
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	/** Devuelve el objeto como texto. */
	@Override
	public String toString() {
		return "AuditAdminDTO [id=" + id + ", username=" + username + ", action=" + action + ", ip=" + ip
				+ ", location=" + location + ", detail=" + detail + ", createdAt=" + createdAt + "]";
	}
	
	
}