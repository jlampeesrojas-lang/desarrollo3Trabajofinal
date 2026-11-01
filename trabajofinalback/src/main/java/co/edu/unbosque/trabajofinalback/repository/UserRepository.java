package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de usuarios. Todas las consultas son derivadas del nombre del método (JPA), por lo
 * que usan parámetros y no concatenan SQL.
 */
public interface UserRepository extends JpaRepository<User, Long> {

  /** Busca un usuario por su nombre de usuario. */
  Optional<User> findByUsername(String username);

  /** Busca un usuario por su correo (guardado en minúsculas). */
  Optional<User> findByEmail(String email);

  /** Indica si ya existe el nombre de usuario. */
  boolean existsByUsername(String username);

  /** Indica si ya existe el correo. */
  boolean existsByEmail(String email);

  /** Elimina un usuario por su nombre de usuario. */
  void deleteByUsername(String username);
}
