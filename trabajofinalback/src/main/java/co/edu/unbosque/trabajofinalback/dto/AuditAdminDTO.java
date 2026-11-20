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

	public AuditAdminDTO() {
		// TODO Auto-generated constructor stub
	}

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

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
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

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public String toString() {
		return "AuditAdminDTO [id=" + id + ", username=" + username + ", action=" + action + ", ip=" + ip
				+ ", location=" + location + ", detail=" + detail + ", createdAt=" + createdAt + "]";
	}
	
	
}
