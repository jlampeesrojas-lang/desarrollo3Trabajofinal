package co.edu.unbosque.trabajofinalback.controller;

import co.edu.unbosque.trabajofinalback.dto.ChatMessageDTO;
import co.edu.unbosque.trabajofinalback.exception.AiApiException;
import co.edu.unbosque.trabajofinalback.security.AuthContext;
import co.edu.unbosque.trabajofinalback.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Asistente de inteligencia artificial. Requiere token JWT. */
@RestController
@RequestMapping("/ai")
@Transactional
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Inteligencia artificial", description = "Asistente de IA de los chats (Groq)")
public class AiController {

	@Autowired
	private AiService aiServ;

	@Autowired
	private AuthContext authContext;

	/** Constructor vacío. */
	public AiController() {
	}

	/** Envía la pregunta del usuario a la IA y devuelve su respuesta. */
	@Operation(summary = "Preguntar a la IA", description = "Body: {\"contenido\": \"¿Qué es Java?\"}. Responde sin guardar nada.")
	@PostMapping(path = "/ask", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> ask(@RequestBody ChatMessageDTO pregunta) {
		String texto = pregunta.getContenido();
		if (texto == null || texto.isBlank()) {
			return new ResponseEntity<>("La pregunta no puede estar vacía", HttpStatus.BAD_REQUEST);
		}
		if (texto.length() > 2000) {
			return new ResponseEntity<>("La pregunta supera los 2000 caracteres", HttpStatus.BAD_REQUEST);
		}
		try {
			return new ResponseEntity<>(aiServ.preguntar(texto.trim()), HttpStatus.OK);
		} catch (AiApiException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_GATEWAY);
		}
	}

	/**
	 * Envía una pregunta a la IA en un chat y devuelve la respuesta o un error.
	 */
	@Operation(summary = "Preguntar a la IA dentro de un chat", description = "Body: {\"contenido\": \"...\"}. Guarda la pregunta y la respuesta cifradas en el chat;"
			+ " la IA tiene en cuenta los últimos mensajes de la conversación.")
	@PostMapping(path = "/askinchat/{chatId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> askInChat(@PathVariable Long chatId, @RequestBody ChatMessageDTO pregunta,
			HttpServletRequest request) {
		int status = aiServ.askInChat(authContext.getUserId(), chatId, pregunta, request.getRemoteAddr());

		if (status == 0) {
			return new ResponseEntity<>(pregunta.getContenido(), HttpStatus.CREATED);
		} else if (status == 1) {
			return new ResponseEntity<>("El chat no existe", HttpStatus.NOT_FOUND);
		} else if (status == 2) {
			return new ResponseEntity<>("No participas en este chat", HttpStatus.FORBIDDEN);
		} else if (status == 3) {
			return new ResponseEntity<>("La pregunta no puede estar vacía", HttpStatus.BAD_REQUEST);
		} else if (status == 4) {
			return new ResponseEntity<>("La pregunta supera los 2000 caracteres", HttpStatus.BAD_REQUEST);
		} else {
			return new ResponseEntity<>("La IA no está disponible en este momento. Intenta más tarde",
					HttpStatus.BAD_GATEWAY);
		}
	}
}