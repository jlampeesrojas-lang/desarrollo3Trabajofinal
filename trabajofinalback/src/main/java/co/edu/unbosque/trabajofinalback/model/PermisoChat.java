package co.edu.unbosque.trabajofinalback.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Permiso temporal de un usuario para leer un chat desbloqueado. */
@Entity
@Table(name = "chat_unlock_permissions")
public class PermisoChat {

    @Id
    @Column(length = 36)
    private String codigo;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private Long chatId;

    @Column(nullable = false)
    private Instant venceEn;

    protected PermisoChat() {
    }

    public PermisoChat(String codigo, Long usuarioId, Long chatId, Instant venceEn) {
        this.codigo = codigo;
        this.usuarioId = usuarioId;
        this.chatId = chatId;
        this.venceEn = venceEn;
    }

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public Long getUsuarioId() {
		return usuarioId;
	}

	public void setUsuarioId(Long usuarioId) {
		this.usuarioId = usuarioId;
	}

	public Long getChatId() {
		return chatId;
	}

	public void setChatId(Long chatId) {
		this.chatId = chatId;
	}

	public Instant getVenceEn() {
		return venceEn;
	}

	public void setVenceEn(Instant venceEn) {
		this.venceEn = venceEn;
	}
}