package co.edu.unbosque.trabajofinalback.controller;

import co.edu.unbosque.trabajofinalback.dto.ContactDTO;
import co.edu.unbosque.trabajofinalback.security.AuthContext;
import co.edu.unbosque.trabajofinalback.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administra la lista de contactos del usuario autenticado. */
@RestController
@RequestMapping("/contacts")
@Transactional
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Contactos", description = "Lista de contactos del usuario")
public class ContactController {

    @Autowired
    private ContactService contactServ;

    @Autowired
    private AuthContext authContext;

    /** Crea el controlador de contactos. */
    public ContactController() {
    }

    /** Agrega un contacto a partir de su nombre de usuario o correo electrónico. */
    @Operation(
            summary = "Agregar contacto",
            description = "Body: {\"usuario\": \"nombre de usuario o correo\", \"alias\": \"opcional\"}")
    @PostMapping(path = "/createjson", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createNewWithJSON(@RequestBody ContactDTO newContact) {
        if (newContact.getUsuario() != null
                && (newContact.getUsuario().contains("<") || newContact.getUsuario().contains(">"))) {
            return new ResponseEntity<>("Solicitud con caracteres invalidos", HttpStatus.BAD_REQUEST);
        }

        int status = contactServ.create(authContext.getUserId(), newContact);

        if (status == 0) {
            return new ResponseEntity<>("Contacto agregado exitosamente", HttpStatus.CREATED);
        } else if (status == 1) {
            return new ResponseEntity<>("No existe un usuario con ese nombre o correo", HttpStatus.NOT_FOUND);
        } else if (status == 2) {
            return new ResponseEntity<>("No puedes agregarte a ti mismo", HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<>("Ese usuario ya está en tus contactos", HttpStatus.NOT_ACCEPTABLE);
        }
    }

    /** Devuelve la lista de contactos del usuario autenticado. */
    @Operation(summary = "Listar mis contactos")
    @GetMapping("/getall")
    public ResponseEntity<List<ContactDTO>> getAll() {
        List<ContactDTO> contacts = contactServ.getAllByOwner(authContext.getUserId());
        return new ResponseEntity<>(contacts, HttpStatus.OK);
    }

    /** Quita de la lista del usuario autenticado el contacto indicado. */
    @Operation(summary = "Eliminar contacto", description = "id = id del usuario contacto")
    @DeleteMapping("/deletebyid/{id}")
    public ResponseEntity<String> deleteById(@PathVariable Long id) {
        int status = contactServ.deleteByContactId(authContext.getUserId(), id);

        if (status == 0) {
            return new ResponseEntity<>("Contacto eliminado exitosamente", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Ese usuario no está en tus contactos", HttpStatus.NOT_FOUND);
        }
    }
}