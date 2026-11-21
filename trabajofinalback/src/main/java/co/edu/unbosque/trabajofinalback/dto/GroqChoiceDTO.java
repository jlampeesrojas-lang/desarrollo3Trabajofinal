package co.edu.unbosque.trabajofinalback.dto;

/** Una de las respuestas generadas por la API de Groq. */
public class GroqChoiceDTO {

  private GroqMessageDTO message;

  public GroqChoiceDTO() {}

  public GroqMessageDTO getMessage() {
    return message;
  }

  public void setMessage(GroqMessageDTO message) {
    this.message = message;
  }
}