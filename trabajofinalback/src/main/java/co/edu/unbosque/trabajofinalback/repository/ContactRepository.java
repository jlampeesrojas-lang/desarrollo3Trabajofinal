package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.Contact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de contactos. */
public interface ContactRepository extends JpaRepository<Contact, Long> {

  /** Contactos de un usuario (con los datos del contacto ya cargados). */
  @EntityGraph(attributePaths = "contactUser")
  List<Contact> findByOwnerIdOrderByCreatedAtAsc(Long ownerId);

  @EntityGraph(attributePaths = "contactUser")
  Optional<Contact> findByOwnerIdAndContactUserId(Long ownerId, Long contactUserId);

  boolean existsByOwnerIdAndContactUserId(Long ownerId, Long contactUserId);
}