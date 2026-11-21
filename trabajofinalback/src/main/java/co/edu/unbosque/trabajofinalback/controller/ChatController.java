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

/** Chats y mensajes del usuario que inició sesión. Requiere token JWT. */
@RestController
@RequestMapping("/chats")
@Transactional
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chats", description = "Chats 1 a 1 y mensajes cifrados")
public class ChatController {

  @Autowired private ChatService chatServ;

  @Autowired private AuthContext authContext;

  public ChatController() {}

  /**
   * Crea un chat con un contacto a partir de los datos JSON proporcionados.
   */
  @Operation(
      summary = "Crear chat con un contacto",
      description = "Body: {\"contactoId\": 1}. Genera un código único y una llave AES propia.")
  @PostMapping(path = "/createjson", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> createNewWithJSON(@RequestBody ChatDTO newChat) {
    int status = chatServ.create(authContext.getUserId(), newChat);

    if (status == 0) {
      return new ResponseEntity<>(
          "Chat creado exitosamente. Id: " + newChat.getId() + ", código: " + newChat.getCodigo(),
          HttpStatus.CREATED);
    } else if (status == 1) {
      return new ResponseEntity<>("El usuario no existe", HttpStatus.NOT_FOUND);
    } else if (status == 2) {
      return new ResponseEntity<>("No puedes crear un chat contigo mismo", HttpStatus.BAD_REQUEST);
    } else if (status == 3) {
      return new ResponseEntity<>(
          "Primero agrega a este usuario a tus contactos", HttpStatus.BAD_REQUEST);
    } else {
      return new ResponseEntity<>(
          "Ya tienes un chat con este contacto. Id: "
              + newChat.getId()
              + ", código: "
              + newChat.getCodigo(),
          HttpStatus.NOT_ACCEPTABLE);
    }
  }

  @Operation(summary = "Listar mis chats", description = "El de actividad más reciente primero")
  @GetMapping("/getall")
  public ResponseEntity<List<ChatDTO>> getAll() {
    List<ChatDTO> chats = chatServ.getAllByUser(authContext.getUserId());
    return new ResponseEntity<>(chats, HttpStatus.OK);
  }

  @Operation(summary = "Ver un chat", description = "403 si no participas, 404 si no existe")
  @GetMapping("/getbyid/{id}")
  public ResponseEntity<ChatDTO> getById(@PathVariable Long id) {
    int acceso = chatServ.checkAccess(authContext.getUserId(), id);
    if (acceso == 1) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } else if (acceso == 2) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    ChatDTO chat = chatServ.getById(authContext.getUserId(), id);
    return new ResponseEntity<>(chat, HttpStatus.OK);
  }

  /**
   * Envía un mensaje de texto al chat indicado y devuelve el resultado de la operación.
   */
  @Operation(
      summary = "Enviar mensaje de texto",
      description = "Body: {\"contenido\": \"Hola\"}. Se guarda cifrado con la llave del chat.")
  @PostMapping(path = "/sendmessage/{chatId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> sendMessage(
      @PathVariable Long chatId,
      @RequestBody ChatMessageDTO newMessage,
      HttpServletRequest request) {
    int status =
        chatServ.sendMessage(
            authContext.getUserId(), chatId, newMessage, request.getRemoteAddr());

    if (status == 0) {
      return new ResponseEntity<>("Mensaje enviado exitosamente", HttpStatus.CREATED);
    } else if (status == 1) {
      return new ResponseEntity<>("El chat no existe", HttpStatus.NOT_FOUND);
    } else if (status == 2) {
      return new ResponseEntity<>("No participas en este chat", HttpStatus.FORBIDDEN);
    } else if (status == 3) {
      return new ResponseEntity<>("El mensaje no puede estar vacío", HttpStatus.BAD_REQUEST);
    } else {
      return new ResponseEntity<>(
          "El mensaje supera los 2000 caracteres", HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Obtiene el historial paginado de mensajes de un chat si el usuario tiene acceso.
   */
  @Operation(
      summary = "Historial de mensajes (paginado)",
      description = "page 0 = los más recientes. size = mensajes por página (máx. 50)")
  @GetMapping("/getmessages/{chatId}")
  public ResponseEntity<List<ChatMessageDTO>> getMessages(
      @PathVariable Long chatId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    int acceso = chatServ.checkAccess(authContext.getUserId(), chatId);
    if (acceso == 1) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } else if (acceso == 2) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    List<ChatMessageDTO> mensajes = chatServ.getMessages(chatId, page, size);
    return new ResponseEntity<>(mensajes, HttpStatus.OK);
  }

  @Operation(
      summary = "Mensajes nuevos de un chat",
      description = "Devuelve los mensajes con id mayor a afterId, del más antiguo al más reciente")
  @GetMapping("/getnewmessages/{chatId}")
  public ResponseEntity<List<ChatMessageDTO>> getNewMessages(
      @PathVariable Long chatId, @RequestParam(defaultValue = "0") Long afterId) {
    int acceso = chatServ.checkAccess(authContext.getUserId(), chatId);
    if (acceso == 1) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } else if (acceso == 2) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    List<ChatMessageDTO> nuevos = chatServ.getNewMessages(chatId, afterId);
    return new ResponseEntity<>(nuevos, HttpStatus.OK);
  }

  @Operation(summary = "Mensajes enviados", description = "En todos los chats del usuario")
  @GetMapping("/getsent")
  public ResponseEntity<List<ChatMessageDTO>> getSent(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    List<ChatMessageDTO> enviados = chatServ.getSent(authContext.getUserId(), page, size);
    return new ResponseEntity<>(enviados, HttpStatus.OK);
  }

  @Operation(
      summary = "Mensajes recibidos",
      description = "En todos los chats del usuario. Con afterId solo trae los más recientes")
  @GetMapping("/getreceived")
  public ResponseEntity<List<ChatMessageDTO>> getReceived(
      @RequestParam(defaultValue = "0") Long afterId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    List<ChatMessageDTO> recibidos =
        chatServ.getReceived(authContext.getUserId(), afterId, page, size);
    return new ResponseEntity<>(recibidos, HttpStatus.OK);
  }

  /**
   * Marca el chat como leído para el usuario autenticado.
   */
  @Operation(summary = "Marcar chat como leído")
  @PutMapping("/markasread/{chatId}")
  public ResponseEntity<String> markAsRead(@PathVariable Long chatId) {
    int status = chatServ.markAsRead(authContext.getUserId(), chatId);

    if (status == 0) {
      return new ResponseEntity<>("Chat marcado como leído", HttpStatus.OK);
    } else {
      return new ResponseEntity<>("No participas en este chat", HttpStatus.NOT_FOUND);
    }
  }
}