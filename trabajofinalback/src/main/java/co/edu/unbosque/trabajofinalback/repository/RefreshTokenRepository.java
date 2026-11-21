package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.RefreshToken;
import co.edu.unbosque.trabajofinalback.model.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Transactional;

/** Repositorio de tokens de renovación de sesión. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	void deleteByUser(User user);

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/** Obtiene los tokens de renovación que siguen activos. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<RefreshToken> findByUserAndRevokedFalse(User user);

	/** Revoca las sesiones activas del usuario. */
	@Transactional
	default int revokeAllByUser(User user) {
		List<RefreshToken> activos = findByUserAndRevokedFalse(user);

		for (RefreshToken token : activos) {
			token.setRevoked(true);
		}

		flush();
		return activos.size();
	}
}