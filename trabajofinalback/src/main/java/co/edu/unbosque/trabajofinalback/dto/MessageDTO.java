package co.edu.unbosque.trabajofinalback.dto;

/** Respuesta simple con un mensaje de texto. */
public class MessageDTO {

  private String message;

  public MessageDTO() {}

  public MessageDTO(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
