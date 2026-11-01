package co.edu.unbosque.trabajofinalback.dto;

import jakarta.validation.constraints.NotBlank;

/** Credenciales de inicio de sesión. El identificador puede ser el correo o el nombre de usuario. */
public class LoginDTO {

  @NotBlank(message = "El correo o usuario es obligatorio")
  private String identifier;

  @NotBlank(message = "La contraseña es obligatoria")
  private String password;

  public LoginDTO() {}

  public LoginDTO(String identifier, String password) {
    this.identifier = identifier;
    this.password = password;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
