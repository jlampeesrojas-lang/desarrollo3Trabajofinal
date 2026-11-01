package co.edu.unbosque.trabajofinalback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para registrar un usuario nuevo. El rol siempre será USER. */
public class RegisterDTO {

  @NotBlank(message = "El nombre de usuario es obligatorio")
  @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
  @Pattern(
      regexp = "^[A-Za-z0-9_.-]+$",
      message = "El nombre de usuario solo admite letras, números, punto, guion y guion bajo")
  private String username;

  @NotBlank(message = "El correo es obligatorio")
  @Email(message = "El correo no tiene un formato válido")
  @Size(max = 150, message = "El correo es demasiado largo")
  private String email;

  @NotBlank(message = "La contraseña es obligatoria")
  @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
  private String password;

  public RegisterDTO() {}

  public RegisterDTO(String username, String email, String password) {
    this.username = username;
    this.email = email;
    this.password = password;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
