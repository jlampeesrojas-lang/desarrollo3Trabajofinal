package co.edu.unbosque.trabajofinalback.controller;

import co.edu.unbosque.trabajofinalback.dto.ChatMessageDTO;
import co.edu.unbosque.trabajofinalback.exception.AiApiException;
import co.edu.unbosque.trabajofinalback.security.AuthContext;
import co.edu.unbosque.trabajofinalback.service.AiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Atiende las preguntas directas y las conversaciones guardadas con la IA. */
@RestController
@RequestMapping("/ai")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Inteligencia artificial", description = "Asistente de IA de los chats (Groq)")
public class AiController {

	private final AiService aiServ;
	private final AuthContext authContext;

	/** Recibe el servicio de IA y la sesión del usuario. */
	public AiController(AiService aiServ, AuthContext authContext) {
		this.aiServ = aiServ;
		this.authContext = authContext;
	}

	/** Responde una pregunta directa sin consultar el historial de un chat. */
	@Operation(summary = "Preguntar a la IA sin guardar historial")
	@PostMapping(path = "/ask", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> ask(@RequestBody ChatMessageDTO pregunta) {
		String texto = pregunta.getContenido();

		if (texto == null || texto.isBlank()) {
			return ResponseEntity.badRequest().body("La pregunta no puede estar vacía");
		}
		if (texto.length() > 2000) {
			return ResponseEntity.badRequest().body("La pregunta supera los 2000 caracteres");
		}

		try {
			String respuesta = aiServ.preguntar(texto.trim());
			return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(respuesta);
		} catch (AiApiException error) {
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error.getMessage());
		}
	}

	/**
	 * Consulta a la IA con el historial y guarda la pregunta y la respuesta juntas.
	 */
	@Operation(summary = "Preguntar a la IA dentro de un chat desbloqueado")
	@Transactional
	@PostMapping(path = "/askinchat/{chatId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> askInChat(@PathVariable("chatId") Long chatId, @RequestBody ChatMessageDTO pregunta,
			HttpServletRequest request) {

		int estado = aiServ.askInChat(authContext.getUserId(), chatId, pregunta, request.getRemoteAddr());

		if (estado == 0) {
			return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
					.body(pregunta.getContenido());
		}
		if (estado == 1) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El chat no existe");
		}
		if (estado == 2) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No participas en este chat");
		}
		if (estado == 3) {
			return ResponseEntity.badRequest().body("La pregunta no puede estar vacía");
		}
		if (estado == 4) {
			return ResponseEntity.badRequest().body("La pregunta supera los 2000 caracteres");
		}

		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body("La IA no está disponible en este momento. Intenta más tarde.");
	}

	/**
	 * Hace que la IA responda al último mensaje del chat: una imagen, una nota de
	 * voz, un GIF, un archivo o un texto ya enviados, junto con el texto opcional
	 * que escribió el usuario. Se ejecuta dentro de una transacción para poder leer
	 * el chat y el archivo del mensaje.
	 */
	@Operation(summary = "Respuesta de la IA al último mensaje del chat", description = "Primero se envía el adjunto con /media/chat/{chatId} o el GIF con"
			+ " /chats/sendmessage/{chatId}; luego se llama esta ruta. Body opcional: {\"contenido\": \"texto"
			+ " escrito junto con el adjunto\"}. La IA entiende imágenes, notas de voz, GIF y archivos de texto.")
	@Transactional
	@PostMapping("/reply/{chatId}")
	public ResponseEntity<String> reply(@PathVariable Long chatId,
			@RequestBody(required = false) ChatMessageDTO pregunta, HttpServletRequest request) {
		String texto = pregunta == null ? null : pregunta.getContenido();
		ChatMessageDTO respuesta = new ChatMessageDTO();
		int status = aiServ.reply(authContext.getUserId(), chatId, texto, respuesta, request.getRemoteAddr());

		if (status == 0) {
			return new ResponseEntity<>(respuesta.getContenido(), HttpStatus.CREATED);
		} else if (status == 1) {
			return new ResponseEntity<>("El chat no existe", HttpStatus.NOT_FOUND);
		} else if (status == 2) {
			return new ResponseEntity<>("No participas en este chat", HttpStatus.FORBIDDEN);
		} else if (status == 3) {
			return new ResponseEntity<>("No hay ningún mensaje tuyo para responder", HttpStatus.BAD_REQUEST);
		} else if (status == 4) {
			return new ResponseEntity<>("El texto supera los 2000 caracteres", HttpStatus.BAD_REQUEST);
		} else {
			return new ResponseEntity<>("La IA no está disponible en este momento. Intenta más tarde",
					HttpStatus.BAD_GATEWAY);
		}
	}
}