package co.edu.unbosque.trabajofinalback.controller;

import co.edu.unbosque.trabajofinalback.dto.ChatDTO;
import co.edu.unbosque.trabajofinalback.dto.ChatMessageDTO;
import co.edu.unbosque.trabajofinalback.security.AuthContext;
import co.edu.unbosque.trabajofinalback.service.ChatService;
import co.edu.unbosque.trabajofinalback.service.ChatUnlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Atiende las consultas y los envíos de mensajes del usuario autenticado. */
@RestController
@RequestMapping("/chats")
@Transactional
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chats", description = "Chats 1 a 1 y mensajes cifrados")
public class ChatController {

	private final ChatService chatServ;
	private final ChatUnlockService permisos;
	private final AuthContext authContext;

	/** Recibe los servicios de conversaciones, desbloqueo y sesión. */
	public ChatController(ChatService chatServ, ChatUnlockService permisos, AuthContext authContext) {
		this.chatServ = chatServ;
		this.permisos = permisos;
		this.authContext = authContext;
	}

	/** Crea una conversación con un contacto del usuario. */
	@Operation(summary = "Crear chat con un contacto")
	@PostMapping(path = "/createjson", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> createNewWithJSON(@RequestBody ChatDTO newChat) {
		int estado = chatServ.create(authContext.getUserId(), newChat);

		if (estado == 0) {
			return ResponseEntity.status(HttpStatus.CREATED)
					.body("Chat creado exitosamente. Id: " + newChat.getId() + ", código: " + newChat.getCodigo());
		}
		if (estado == 1) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El usuario no existe");
		}
		if (estado == 2) {
			return ResponseEntity.badRequest().body("No puedes crear un chat contigo mismo");
		}
		if (estado == 3) {
			return ResponseEntity.badRequest().body("Primero agrega a este usuario a tus contactos");
		}

		return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(
				"Ya tienes un chat con este contacto. Id: " + newChat.getId() + ", código: " + newChat.getCodigo());
	}

	/** Lista las conversaciones del usuario. */
	@Operation(summary = "Listar mis chats")
	@GetMapping("/getall")
	public ResponseEntity<List<ChatDTO>> getAll() {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.body(chatServ.getAllByUser(authContext.getUserId()));
	}

	/** Consulta una conversación en la que participa el usuario. */
	@Operation(summary = "Ver un chat")
	@GetMapping("/getbyid/{id}")
	public ResponseEntity<ChatDTO> getById(@PathVariable("id") Long id) {
		Long usuarioId = authContext.getUserId();
		int acceso = chatServ.checkAccess(usuarioId, id);

		if (acceso == 1) {
			return ResponseEntity.notFound().build();
		}
		if (acceso == 2) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(chatServ.getById(usuarioId, id));
	}

	/** Envía texto o GIF desde una conversación desbloqueada. */
	@Operation(summary = "Enviar un mensaje de texto o un GIF")
	@PostMapping(path = "/sendmessage/{chatId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> sendMessage(@PathVariable("chatId") Long chatId, @RequestBody ChatMessageDTO newMessage,
			HttpServletRequest request) {

		Long usuarioId = authContext.getUserId();
		int acceso = chatServ.checkAccess(usuarioId, chatId);

		if (acceso == 1) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El chat no existe");
		}
		if (acceso == 2) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No participas en este chat");
		}
		if (!permisos.abierto(chatId)) {
			return ResponseEntity.status(HttpStatus.LOCKED).body("Desbloquea el chat para enviar mensajes.");
		}

		int estado = chatServ.sendMessage(usuarioId, chatId, newMessage, request.getRemoteAddr());

		if (estado == 0) {
			return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(newMessage);
		}
		if (estado == 1) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El chat no existe");
		}
		if (estado == 2) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No participas en este chat");
		}
		if (estado == 3) {
			return ResponseEntity.badRequest().body("El mensaje no puede estar vacío");
		}
		if (estado == 4) {
			return ResponseEntity.badRequest().body("El mensaje supera los 2000 caracteres");
		}
		if (estado == 5) {
			return ResponseEntity.badRequest().body("El enlace del GIF no es válido");
		}

		return ResponseEntity.badRequest().body("Tipo de mensaje no permitido en esta ruta");
	}

	/** Marca como eliminado un mensaje propio; el contenido se conserva en el historial. */
	@Operation(summary = "Eliminar un mensaje propio")
	@DeleteMapping("/deletemessage/{messageId}")
	public ResponseEntity<String> deleteMessage(@PathVariable("messageId") Long messageId,
			HttpServletRequest request) {

		int estado = chatServ.deleteMessage(authContext.getUserId(), messageId, request.getRemoteAddr());

		if (estado == 0) {
			return ResponseEntity.ok("Mensaje eliminado");
		}
		if (estado == 1) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El mensaje no existe");
		}
		if (estado == 2) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo puedes eliminar tus propios mensajes");
		}
		if (estado == 3) {
			return ResponseEntity.badRequest().body("El mensaje ya estaba eliminado");
		}

		return ResponseEntity.status(HttpStatus.LOCKED).body("Desbloquea el chat para eliminar mensajes.");
	}

	/** Devuelve el historial visible para la pestaña actual. */
	@Operation(summary = "Historial de mensajes paginado")
	@GetMapping("/getmessages/{chatId}")
	public ResponseEntity<List<ChatMessageDTO>> getMessages(@PathVariable("chatId") Long chatId,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {

		int acceso = chatServ.checkAccess(authContext.getUserId(), chatId);

		if (acceso == 1) {
			return ResponseEntity.notFound().build();
		}
		if (acceso == 2) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(chatServ.getMessages(chatId, page, size));
	}

	/** Devuelve los mensajes posteriores al último ID recibido. */
	@Operation(summary = "Mensajes nuevos de un chat")
	@GetMapping("/getnewmessages/{chatId}")
	public ResponseEntity<List<ChatMessageDTO>> getNewMessages(@PathVariable("chatId") Long chatId,
			@RequestParam(name = "afterId", defaultValue = "0") Long afterId) {

		int acceso = chatServ.checkAccess(authContext.getUserId(), chatId);

		if (acceso == 1) {
			return ResponseEntity.notFound().build();
		}
		if (acceso == 2) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(chatServ.getNewMessages(chatId, afterId));
	}

	/** Lista los mensajes enviados por el usuario. */
	@Operation(summary = "Mensajes enviados")
	@GetMapping("/getsent")
	public ResponseEntity<List<ChatMessageDTO>> getSent(@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {

		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.body(chatServ.getSent(authContext.getUserId(), page, size));
	}

	/** Lista los mensajes recibidos por el usuario. */
	@Operation(summary = "Mensajes recibidos")
	@GetMapping("/getreceived")
	public ResponseEntity<List<ChatMessageDTO>> getReceived(
			@RequestParam(name = "afterId", defaultValue = "0") Long afterId,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {

		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.body(chatServ.getReceived(authContext.getUserId(), afterId, page, size));
	}

	/** Actualiza la fecha de lectura del usuario. */
	@Operation(summary = "Marcar chat como leído")
	@PutMapping("/markasread/{chatId}")
	public ResponseEntity<String> markAsRead(@PathVariable("chatId") Long chatId) {
		int estado = chatServ.markAsRead(authContext.getUserId(), chatId);
		if (estado == 0) {
			return ResponseEntity.ok("Chat marcado como leído");
		}

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No participas en este chat");
	}

	/** Devuelve el ID de la conversación privada del usuario con la IA. */
	@Operation(summary = "Abrir mi chat privado con la IA")
	@PostMapping("/ia")
	public ResponseEntity<Long> abrirChatIa() {
		return ResponseEntity.ok(chatServ.abrirChatIa(authContext.getUserId()));
	}
}