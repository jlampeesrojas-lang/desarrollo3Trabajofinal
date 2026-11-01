package co.edu.unbosque.trabajofinalback.dto;

import co.edu.unbosque.trabajofinalback.model.User.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * DTO de usuario para operaciones de administración. La contraseña es de SOLO ESCRITURA: se acepta
 * al recibir una petición, pero nunca se devuelve en una respuesta.
 */
public class UserDTO {

  /** Identificador único del usuario. */
  private Long id;

  /** Nombre de usuario. */
  private String username;

  /** Correo electrónico. */
  private String email;

  /** Contraseña en texto plano recibida del cliente (solo escritura, nunca se devuelve). */
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String password;

  /** Rol del usuario (USER, ADMIN). */
  private Role role;

  public UserDTO() {}

  public UserDTO(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public UserDTO(String username, String password, Role role) {
    this(username, password);
    this.role = role;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, username, email, role);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    UserDTO other = (UserDTO) obj;
    return Objects.equals(id, other.id)
        && Objects.equals(username, other.username)
        && Objects.equals(email, other.email)
        && role == other.role;
  }

  /** No incluye la contraseña. */
  @Override
  public String toString() {
    return "UserDTO [id=" + id + ", username=" + username + ", email=" + email + ", role=" + role + "]";
  }
}
