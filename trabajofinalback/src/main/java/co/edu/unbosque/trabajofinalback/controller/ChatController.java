package co.edu.unbosque.trabajofinalback.controller;

import co.edu.unbosque.trabajofinalback.dto.ChatDTO;
import co.edu.unbosque.trabajofinalback.dto.ChatMessageDTO;
import co.edu.unbosque.trabajofinalback.security.AuthContext;
import co.edu.unbosque.trabajofinalback.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Chats y mensajes del usuario que inició sesión. */
@RestController
@RequestMapping("/chats")
@Transactional
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chats", description = "Chats 1 a 1 y mensajes cifrados")
public class ChatController {

    @Autowired
    private ChatService chatServ;

    @Autowired
    private AuthContext authContext;

    /** Crea el controlador de conversaciones. */
    public ChatController() {
    }

    /** Crea una conversación con un contacto del usuario autenticado. */
    @Operation(summary = "Crear chat con un contacto",
            description = "Body: {\"contactoId\": 1}. Genera un código y una llave para el chat.")
    @PostMapping(path = "/createjson", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createNewWithJSON(@RequestBody ChatDTO newChat) {
        int status = chatServ.create(authContext.getUserId(), newChat);

        if (status == 0) {
            return new ResponseEntity<>(
                    "Chat creado exitosamente. Id: " + newChat.getId()
                            + ", código: " + newChat.getCodigo(),
                    HttpStatus.CREATED);
        }
        if (status == 1) {
            return new ResponseEntity<>("El usuario no existe", HttpStatus.NOT_FOUND);
        }
        if (status == 2) {
            return new ResponseEntity<>("No puedes crear un chat contigo mismo",
                    HttpStatus.BAD_REQUEST);
        }
        if (status == 3) {
            return new ResponseEntity<>("Primero agrega a este usuario a tus contactos",
                    HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(
                "Ya tienes un chat con este contacto. Id: " + newChat.getId()
                        + ", código: " + newChat.getCodigo(),
                HttpStatus.NOT_ACCEPTABLE);
    }

    /** Lista los chats del usuario por actividad reciente. */
    @Operation(summary = "Listar mis chats")
    @GetMapping("/getall")
    public ResponseEntity<List<ChatDTO>> getAll() {
        return ResponseEntity.ok(chatServ.getAllByUser(authContext.getUserId()));
    }

    /** Consulta un chat al que pertenece el usuario. */
    @Operation(summary = "Ver un chat")
    @GetMapping("/getbyid/{id}")
    public ResponseEntity<ChatDTO> getById(@PathVariable("id") Long id) {
        Long userId = authContext.getUserId();
        int acceso = chatServ.checkAccess(userId, id);

        if (acceso == 1) {
            return ResponseEntity.notFound().build();
        }
        if (acceso == 2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(chatServ.getById(userId, id));
    }

    /** Guarda un texto o GIF y devuelve el mensaje confirmado al frontend. */
    @Operation(summary = "Enviar un mensaje de texto o un GIF")
    @PostMapping(path = "/sendmessage/{chatId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sendMessage(
            @PathVariable("chatId") Long chatId,
            @RequestBody ChatMessageDTO newMessage,
            HttpServletRequest request) {

        int status = chatServ.sendMessage(
                authContext.getUserId(), chatId, newMessage, request.getRemoteAddr());

        if (status == 0) {
            return new ResponseEntity<>(newMessage, HttpStatus.CREATED);
        }
        if (status == 1) {
            return new ResponseEntity<>("El chat no existe", HttpStatus.NOT_FOUND);
        }
        if (status == 2) {
            return new ResponseEntity<>("No participas en este chat", HttpStatus.FORBIDDEN);
        }
        if (status == 3) {
            return ResponseEntity.badRequest().body("El mensaje no puede estar vacío");
        }
        if (status == 4) {
            return ResponseEntity.badRequest().body("El mensaje supera los 2000 caracteres");
        }
        if (status == 5) {
            return ResponseEntity.badRequest().body("El enlace del GIF no es válido");
        }
        return ResponseEntity.badRequest().body("Tipo de mensaje no permitido en esta ruta");
    }

    /** Obtiene una página del historial, con los mensajes recientes primero. */
    @Operation(summary = "Historial de mensajes paginado")
    @GetMapping("/getmessages/{chatId}")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(
            @PathVariable("chatId") Long chatId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        int acceso = chatServ.checkAccess(authContext.getUserId(), chatId);
        if (acceso == 1) {
            return ResponseEntity.notFound().build();
        }
        if (acceso == 2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(chatServ.getMessages(chatId, page, size));
    }

    /** Obtiene los mensajes posteriores al último ID que tiene el cliente. */
    @Operation(summary = "Mensajes nuevos de un chat")
    @GetMapping("/getnewmessages/{chatId}")
    public ResponseEntity<List<ChatMessageDTO>> getNewMessages(
            @PathVariable("chatId") Long chatId,
            @RequestParam(name = "afterId", defaultValue = "0") Long afterId) {

        int acceso = chatServ.checkAccess(authContext.getUserId(), chatId);
        if (acceso == 1) {
            return ResponseEntity.notFound().build();
        }
        if (acceso == 2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(chatServ.getNewMessages(chatId, afterId));
    }

    /** Lista los mensajes enviados por el usuario en sus conversaciones. */
    @Operation(summary = "Mensajes enviados")
    @GetMapping("/getsent")
    public ResponseEntity<List<ChatMessageDTO>> getSent(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        return ResponseEntity.ok(chatServ.getSent(authContext.getUserId(), page, size));
    }

    /** Lista los mensajes recibidos por el usuario. */
    @Operation(summary = "Mensajes recibidos")
    @GetMapping("/getreceived")
    public ResponseEntity<List<ChatMessageDTO>> getReceived(
            @RequestParam(name = "afterId", defaultValue = "0") Long afterId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        return ResponseEntity.ok(
                chatServ.getReceived(authContext.getUserId(), afterId, page, size));
    }

    /** Marca como leído el chat para el usuario que hizo la petición. */
    @Operation(summary = "Marcar chat como leído")
    @PutMapping("/markasread/{chatId}")
    public ResponseEntity<String> markAsRead(@PathVariable("chatId") Long chatId) {
        int status = chatServ.markAsRead(authContext.getUserId(), chatId);

        if (status == 0) {
            return ResponseEntity.ok("Chat marcado como leído");
        }
        return new ResponseEntity<>("No participas en este chat", HttpStatus.NOT_FOUND);
    }
}