package co.edu.unbosque.trabajofinalback.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de /auth/confirm-email: el token que llegó en el enlace del correo. */
public class ConfirmEmailDTO {

  @NotBlank(message = "El token es obligatorio")
  private String token;

  public ConfirmEmailDTO() {}

  public ConfirmEmailDTO(String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}
