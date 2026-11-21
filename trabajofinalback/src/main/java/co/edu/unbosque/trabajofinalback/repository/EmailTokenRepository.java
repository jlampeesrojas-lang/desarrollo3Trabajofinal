package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.EmailToken;
import co.edu.unbosque.trabajofinalback.model.EmailToken.TokenType;
import co.edu.unbosque.trabajofinalback.model.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** Repositorio de códigos de confirmación y recuperación de contraseña. */
public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

	void deleteByUser(User user);

	Optional<EmailToken> findByTokenHash(String tokenHash);

	/** Obtiene los tokens del tipo indicado que aún están pendientes. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<EmailToken> findByUserAndTypeAndUsedAtIsNull(User user, TokenType type);
}