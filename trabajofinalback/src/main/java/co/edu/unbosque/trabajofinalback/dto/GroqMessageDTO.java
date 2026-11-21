package co.edu.unbosque.trabajofinalback.dto;

/**
 * Un mensaje de la conversación que se envía o se recibe de la API de Groq. El rol puede ser
 * system (instrucciones), user (persona) o assistant (IA).
 */
public class GroqMessageDTO {

  private String role;
  private String content;

  public GroqMessageDTO() {}

  public GroqMessageDTO(String role, String content) {
    this.role = role;
    this.content = content;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}