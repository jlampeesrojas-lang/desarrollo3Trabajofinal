package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.dto.UserDTO;
import co.edu.unbosque.trabajofinalback.exception.PasswordNotValidException;
import co.edu.unbosque.trabajofinalback.model.User;
import co.edu.unbosque.trabajofinalback.repository.EmailTokenRepository;
import co.edu.unbosque.trabajofinalback.repository.RefreshTokenRepository;
import co.edu.unbosque.trabajofinalback.repository.UserRepository;
import co.edu.unbosque.trabajofinalback.util.PasswordValidator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de gestión de usuarios (uso del administrador). Las contraseñas siempre se guardan como
 * hash BCrypt y nunca se devuelven. El registro público lo maneja AuthService.
 */
@Service
public class UserService implements CRUDOperation<UserDTO> {

  @Autowired private UserRepository userRepo;

  @Autowired private EmailTokenRepository emailTokenRepo;

  @Autowired private RefreshTokenRepository refreshTokenRepo;

  @Autowired private PasswordEncoder passwordEncoder;

  public UserService() {}

  /** Convierte la entidad en DTO SIN contraseña ni hash. */
  private UserDTO toDTO(User user) {
    UserDTO dto = new UserDTO();
    dto.setId(user.getId());
    dto.setUsername(user.getUsername());
    dto.setEmail(user.getEmail());
    dto.setRole(user.getRole());
    return dto;
  }

  @Override
  public long count() {
    return userRepo.count();
  }

  @Override
  public boolean exist(Long id) {
    return userRepo.existsById(id);
  }

  /**
   * Crea un usuario (lo hace el administrador; el correo queda marcado como verificado).
   *
   * @param data DTO con username, email, password y rol opcional
   * @return 0 creado, 1 usuario en uso, 2 correo en uso, 3 datos faltantes, 4 contraseña no válida
   */
  @Override
  public int create(UserDTO data) {
    if (data.getUsername() == null
        || data.getUsername().isBlank()
        || data.getEmail() == null
        || data.getEmail().isBlank()) {
      return 3;
    }
    try {
      PasswordValidator.validate(data.getPassword());
    } catch (PasswordNotValidException e) {
      return 4;
    }
    String email = data.getEmail().trim().toLowerCase();
    if (userRepo.existsByUsername(data.getUsername())) {
      return 1;
    }
    if (userRepo.existsByEmail(email)) {
      return 2;
    }
    User entity =
        new User(
            data.getUsername().trim(),
            email,
            passwordEncoder.encode(data.getPassword()),
            data.getRole() != null ? data.getRole() : User.Role.USER);
    entity.setEmailVerified(true);
    userRepo.save(entity);
    return 0;
  }

  @Override
  public List<UserDTO> getAll() {
    return userRepo.findAll().stream().map(this::toDTO).toList();
  }

  @Override
  @Transactional
  public int deleteById(Long id) {
    Optional<User> found = userRepo.findById(id);
    if (found.isPresent()) {
      deleteWithTokens(found.get());
      return 0;
    }
    return 1;
  }

  /** Borra primero sus tokens (llaves foráneas) y luego el usuario. */
  private void deleteWithTokens(User user) {
    emailTokenRepo.deleteByUser(user);
    refreshTokenRepo.deleteByUser(user);
    userRepo.delete(user);
  }

  /**
   * Elimina un usuario por su nombre de usuario.
   *
   * @return 0 si se eliminó, 1 si no existe
   */
  @Transactional
  public int deleteByUsername(String username) {
    Optional<User> found = userRepo.findByUsername(username);
    if (found.isPresent()) {
      deleteWithTokens(found.get());
      return 0;
    }
    return 1;
  }

  /**
   * Actualiza username, contraseña (opcional) y rol (opcional) de un usuario.
   *
   * @return 0 actualizado, 1 nombre de usuario en uso, 2 usuario no existe, 4 contraseña no válida
   */
  @Override
  public int updateById(Long id, UserDTO newData) {
    Optional<User> found = userRepo.findById(id);
    if (found.isEmpty()) {
      return 2;
    }
    User temp = found.get();
    if (newData.getUsername() != null && !newData.getUsername().isBlank()) {
      Optional<User> other = userRepo.findByUsername(newData.getUsername());
      if (other.isPresent() && !other.get().getId().equals(id)) {
        return 1;
      }
      temp.setUsername(newData.getUsername().trim());
    }
    if (newData.getPassword() != null && !newData.getPassword().isBlank()) {
      try {
        PasswordValidator.validate(newData.getPassword());
      } catch (PasswordNotValidException e) {
        return 4;
      }
      temp.setPasswordHash(passwordEncoder.encode(newData.getPassword()));
    }
    if (newData.getRole() != null) {
      temp.setRole(newData.getRole());
    }
    userRepo.save(temp);
    return 0;
  }

  /** @return DTO del usuario o null si no existe */
  public UserDTO getById(Long id) {
    return userRepo.findById(id).map(this::toDTO).orElse(null);
  }

  /** @return true si el nombre de usuario ya está en uso */
  public boolean findUsernameAlreadyTaken(String username) {
    return userRepo.existsByUsername(username);
  }

  /**
   * Valida las credenciales de un usuario comparando con el hash BCrypt.
   *
   * @return 0 si son válidas, 1 si no
   */
  public int validateCredentials(String username, String password) {
    Optional<User> userOpt = userRepo.findByUsername(username);
    if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPasswordHash())) {
      return 0;
    }
    return 1;
  }
}
