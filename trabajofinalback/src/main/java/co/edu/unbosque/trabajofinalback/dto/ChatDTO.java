package co.edu.unbosque.trabajofinalback.dto;

import java.util.List;

/**
 * Datos de un chat. Para crear solo se envía "contactoId". Al listar, el servidor devuelve el
 * código, los participantes, el último mensaje y cuántos mensajes no ha leído el usuario.
 */
public class ChatDTO {

  private Long id;

  private Long contactoId;

  private String codigo;

  private List<ContactDTO> participantes;

  private ChatMessageDTO ultimoMensaje;

  private long noLeidos;

  private String fechaCreacion;

  public ChatDTO() {}

  public ChatDTO(Long contactoId) {
    this.contactoId = contactoId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getContactoId() {
    return contactoId;
  }

  public void setContactoId(Long contactoId) {
    this.contactoId = contactoId;
  }

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String codigo) {
    this.codigo = codigo;
  }

  public List<ContactDTO> getParticipantes() {
    return participantes;
  }

  public void setParticipantes(List<ContactDTO> participantes) {
    this.participantes = participantes;
  }

  public ChatMessageDTO getUltimoMensaje() {
    return ultimoMensaje;
  }

  public void setUltimoMensaje(ChatMessageDTO ultimoMensaje) {
    this.ultimoMensaje = ultimoMensaje;
  }

  public long getNoLeidos() {
    return noLeidos;
  }

  public void setNoLeidos(long noLeidos) {
    this.noLeidos = noLeidos;
  }

  public String getFechaCreacion() {
    return fechaCreacion;
  }

  public void setFechaCreacion(String fechaCreacion) {
    this.fechaCreacion = fechaCreacion;
  }
}