package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.EmailToken;
import co.edu.unbosque.trabajofinalback.model.EmailToken.TokenType;
import co.edu.unbosque.trabajofinalback.model.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** Repositorio de códigos de confirmación de correo y recuperación de contraseña. */
public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    /** Elimina los códigos de correo asociados al usuario. */
    void deleteByUser(User user);

    /** Busca el código de correo por su hash almacenado. */
    Optional<EmailToken> findByTokenHash(String tokenHash);

    /** Obtiene con bloqueo de escritura los códigos del tipo indicado que siguen sin uso. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<EmailToken> findByUserAndTypeAndUsedAtIsNull(User user, TokenType type);
}