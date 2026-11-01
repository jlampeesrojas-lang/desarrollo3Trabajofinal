package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.EmailToken;
import co.edu.unbosque.trabajofinalback.model.EmailToken.TokenType;
import co.edu.unbosque.trabajofinalback.model.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio de tokens de correo (confirmación y recuperación). */
public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

  /** Borra todos los tokens de un usuario (antes de eliminarlo). */
  void deleteByUser(User user);

  /** Busca un token por el hash SHA-256 del valor enviado por correo. */
  Optional<EmailToken> findByTokenHash(String tokenHash);

  /** Marca como usados todos los tokens pendientes de un usuario y tipo (al reenviar). */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update EmailToken t set t.usedAt = :now "
          + "where t.user = :user and t.type = :type and t.usedAt is null")
  int invalidatePending(
      @Param("user") User user, @Param("type") TokenType type, @Param("now") LocalDateTime now);
}
