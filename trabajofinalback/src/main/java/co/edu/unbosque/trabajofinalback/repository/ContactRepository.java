package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.Contact;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** Repositorio de contactos de cada usuario. */
public interface ContactRepository extends JpaRepository<Contact, Long> {

    /** Lista los contactos del propietario en el orden en que los agregó. */
    @EntityGraph(attributePaths = "contactUser")
    List<Contact> findByOwnerIdOrderByCreatedAtAsc(Long ownerId);

    /** Busca un contacto en la lista del propietario y carga los datos del usuario agregado. */
    @EntityGraph(attributePaths = "contactUser")
    Optional<Contact> findByOwnerIdAndContactUserId(Long ownerId, Long contactUserId);

    /** Comprueba si el propietario ya tiene agregado al usuario indicado. */
    boolean existsByOwnerIdAndContactUserId(Long ownerId, Long contactUserId);

    /** Busca el contacto con bloqueo de escritura durante la transacción de envío. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Contact> findLockedByOwnerIdAndContactUserId(Long ownerId, Long contactUserId);
}