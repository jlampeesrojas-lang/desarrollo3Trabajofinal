package co.edu.unbosque.trabajofinalback.dto;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/** Cuerpo de la petición que se envía a la API de Groq. */
public class GroqRequestDTO {

  private String model;

  private List<GroqMessageDTO> messages;

  /** Qué tan creativa es la respuesta (0 = exacta, 1 = creativa). */
  private Double temperature;

  /** Máximo de tokens que puede generar la IA. */
  @SerializedName("max_completion_tokens")
  private Integer maxCompletionTokens;

  /** Cuánto "piensa" el modelo antes de responder (low, medium o high). */
  @SerializedName("reasoning_effort")
  private String reasoningEffort;

  /** Constructor vacío. */
  public GroqRequestDTO() {}

  /** Crea un GroqRequestDTO con sus datos. */
  public GroqRequestDTO(String model, List<GroqMessageDTO> messages) {
    this.model = model;
    this.messages = messages == null ? new ArrayList<>() : new ArrayList<>(messages);
    this.temperature = 0.7;
    this.maxCompletionTokens = 1024;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public List<GroqMessageDTO> getMessages() {
    return messages;
  }

  public void setMessages(List<GroqMessageDTO> messages) {
    this.messages = messages == null ? new ArrayList<>() : new ArrayList<>(messages);
  }

  public Double getTemperature() {
    return temperature;
  }

  public void setTemperature(Double temperature) {
    this.temperature = temperature;
  }

  public Integer getMaxCompletionTokens() {
    return maxCompletionTokens;
  }

  public void setMaxCompletionTokens(Integer maxCompletionTokens) {
    this.maxCompletionTokens = maxCompletionTokens;
  }

  public String getReasoningEffort() {
    return reasoningEffort;
  }

  public void setReasoningEffort(String reasoningEffort) {
    this.reasoningEffort = reasoningEffort;
  }
}