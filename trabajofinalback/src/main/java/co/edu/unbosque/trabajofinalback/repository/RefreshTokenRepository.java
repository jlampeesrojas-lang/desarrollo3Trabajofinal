package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.RefreshToken;
import co.edu.unbosque.trabajofinalback.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio de refresh tokens. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  /** Borra todas las sesiones de un usuario (antes de eliminarlo). */
  void deleteByUser(User user);

  /** Busca un refresh token por el hash SHA-256 del valor entregado al cliente. */
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /** Revoca todas las sesiones de un usuario (cierre global o reuso sospechoso de un token). */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update RefreshToken r set r.revoked = true where r.user = :user and r.revoked = false")
  int revokeAllByUser(@Param("user") User user);
}
