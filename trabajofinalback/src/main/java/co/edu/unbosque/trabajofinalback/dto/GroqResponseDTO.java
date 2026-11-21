package co.edu.unbosque.trabajofinalback.dto;

import java.util.List;

/** Respuesta de la API de Groq. Solo se leen los campos que usa este back. */
public class GroqResponseDTO {

  private List<GroqChoiceDTO> choices;

  public GroqResponseDTO() {}

  public List<GroqChoiceDTO> getChoices() {
    return choices;
  }

  public void setChoices(List<GroqChoiceDTO> choices) {
    this.choices = choices;
  }
}