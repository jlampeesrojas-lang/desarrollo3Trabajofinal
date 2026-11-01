package co.edu.unbosque.trabajofinalback.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Usuario del sistema. Implementa UserDetails para integrarse con Spring Security.
 *
 * <p>Se guarda en la tabla "users". La contraseña NUNCA se guarda en texto plano: la columna
 * "password_hash" contiene el hash BCrypt.
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

  private static final long serialVersionUID = 1L;

  /** Identificador único (autoincremental). */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Nombre de usuario único. */
  @Column(unique = true, nullable = false, length = 50)
  private String username;

  /** Correo electrónico único (siempre en minúsculas). */
  @Column(unique = true, nullable = false, length = 150)
  private String email;

  /** Hash BCrypt de la contraseña. */
  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  /** Rol del usuario (USER o ADMIN). */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  /** true si el usuario ya confirmó su correo con el enlace enviado. */
  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  /** true si la cuenta no ha expirado. */
  private boolean accountNonExpired;

  /** true si la cuenta no está bloqueada. */
  private boolean accountNonLocked;

  /** true si las credenciales no han expirado. */
  private boolean credentialsNonExpired;

  /** true si la cuenta está habilitada (el administrador puede desactivarla). */
  private boolean enabled;

  /** Fecha de creación de la cuenta. */
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /** Roles disponibles en el sistema. */
  public enum Role {
    /** Usuario normal: usa la plataforma para hablar y gestionar sus contactos. */
    USER,
    /** Administrador: supervisa chats y gestiona usuarios. */
    ADMIN
  }

  /** Constructor por defecto: cuenta activa, rol USER y correo sin confirmar. */
  public User() {
    this.accountNonExpired = true;
    this.accountNonLocked = true;
    this.credentialsNonExpired = true;
    this.enabled = true;
    this.emailVerified = false;
    this.role = Role.USER;
  }

  /**
   * Constructor con rol USER.
   *
   * @param username Nombre de usuario
   * @param email Correo electrónico
   * @param passwordHash Hash BCrypt de la contraseña (nunca el texto plano)
   */
  public User(String username, String email, String passwordHash) {
    this();
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
  }

  /**
   * Constructor completo.
   *
   * @param username Nombre de usuario
   * @param email Correo electrónico
   * @param passwordHash Hash BCrypt de la contraseña (nunca el texto plano)
   * @param role Rol asignado
   */
  public User(String username, String email, String passwordHash, Role role) {
    this(username, email, passwordHash);
    this.role = role;
  }

  /** Asigna la fecha de creación antes del primer guardado. */
  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  /**
   * Requerido por UserDetails: devuelve el HASH de la contraseña, que es lo que Spring Security
   * compara con BCrypt.
   */
  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public boolean isAccountNonExpired() {
    return accountNonExpired;
  }

  @Override
  public boolean isAccountNonLocked() {
    return accountNonLocked;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return credentialsNonExpired;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @Override
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

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public void setAccountNonExpired(boolean accountNonExpired) {
    this.accountNonExpired = accountNonExpired;
  }

  public void setAccountNonLocked(boolean accountNonLocked) {
    this.accountNonLocked = accountNonLocked;
  }

  public void setCredentialsNonExpired(boolean credentialsNonExpired) {
    this.credentialsNonExpired = credentialsNonExpired;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, username);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    User other = (User) obj;
    return Objects.equals(id, other.id) && Objects.equals(username, other.username);
  }

  /** No incluye el hash de la contraseña para que nunca llegue a los logs. */
  @Override
  public String toString() {
    return "User [id=" + id + ", username=" + username + ", email=" + email + ", role=" + role + "]";
  }
}
