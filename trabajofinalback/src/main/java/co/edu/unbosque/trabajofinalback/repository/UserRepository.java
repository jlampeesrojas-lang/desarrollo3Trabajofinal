package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** Repositorio de usuarios. */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Busca una cuenta por su nombre de usuario. */
    Optional<User> findByUsername(String username);

    /** Busca una cuenta por su correo electrónico. */
    Optional<User> findByEmail(String email);

    /** Comprueba si el nombre de usuario ya está registrado. */
    boolean existsByUsername(String username);

    /** Comprueba si el correo electrónico ya está registrado. */
    boolean existsByEmail(String email);

    /** Elimina la cuenta correspondiente al nombre de usuario indicado. */
    void deleteByUsername(String username);

    /** Bloquea el registro del usuario mientras se crea su chat de IA o un contacto. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findLockedById(Long id);
}