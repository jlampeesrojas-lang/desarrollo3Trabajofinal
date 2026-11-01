package co.edu.unbosque.trabajofinalback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Cuerpo de /auth/resend-confirmation. */
public class ResendConfirmationDTO {

  @NotBlank(message = "El correo es obligatorio")
  @Email(message = "El correo no tiene un formato válido")
  private String email;

  public ResendConfirmationDTO() {}

  public ResendConfirmationDTO(String email) {
    this.email = email;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
