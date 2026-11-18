package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.dto.ContactDTO;
import co.edu.unbosque.trabajofinalback.model.Contact;
import co.edu.unbosque.trabajofinalback.model.User;
import co.edu.unbosque.trabajofinalback.repository.ContactRepository;
import co.edu.unbosque.trabajofinalback.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Lógica de los contactos de cada usuario. */
@Service
public class ContactService {

  @Autowired private ContactRepository contactRepo;

  @Autowired private UserRepository userRepo;

  public ContactService() {}

  /**
   * Agrega un contacto buscándolo por nombre de usuario o por correo.
   *
   * @return 0 = agregado, 1 = no existe ese usuario, 2 = es uno mismo, 3 = ya era contacto
   */
  public int create(Long ownerId, ContactDTO data) {
    if (data.getUsuario() == null || data.getUsuario().isBlank()) {
      return 1;
    }
    String buscado = data.getUsuario().trim();

    Optional<User> found = userRepo.findByUsername(buscado);
    if (!found.isPresent()) {
      found = userRepo.findByEmail(buscado.toLowerCase());
    }
    if (!found.isPresent()) {
      return 1;
    }

    User contactUser = found.get();
    if (contactUser.getId().equals(ownerId)) {
      return 2;
    }
    if (contactRepo.existsByOwnerIdAndContactUserId(ownerId, contactUser.getId())) {
      return 3;
    }

    User owner = userRepo.findById(ownerId).get();
    String alias = data.getAlias();
    if (alias != null && alias.isBlank()) {
      alias = null;
    }
    contactRepo.save(new Contact(owner, contactUser, alias));
    return 0;
  }

  /** Contactos del usuario, en el orden en que los agregó. */
  public List<ContactDTO> getAllByOwner(Long ownerId) {
    List<Contact> entityList = contactRepo.findByOwnerIdOrderByCreatedAtAsc(ownerId);
    List<ContactDTO> dtoList = new ArrayList<>();
    entityList.forEach(
        (entity) -> {
          dtoList.add(toDTO(entity));
        });
    return dtoList;
  }

  /**
   * Quita un contacto de la lista.
   *
   * @param contactUserId id del usuario contacto
   * @return 0 = eliminado, 1 = no estaba en los contactos
   */
  public int deleteByContactId(Long ownerId, Long contactUserId) {
    Optional<Contact> found = contactRepo.findByOwnerIdAndContactUserId(ownerId, contactUserId);
    if (found.isPresent()) {
      contactRepo.delete(found.get());
      return 0;
    } else {
      return 1;
    }
  }

  /** Pasa la entidad al DTO (a mano, porque los nombres de los campos son distintos). */
  private ContactDTO toDTO(Contact entity) {
    User u = entity.getContactUser();
    String nombre = entity.getAlias() != null ? entity.getAlias() : u.getUsername();
    return new ContactDTO(u.getId(), u.getUsername(), nombre, entity.getAlias());
  }
}