package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.dto.ChatMessageDTO;
import co.edu.unbosque.trabajofinalback.dto.GroqMessageDTO;
import co.edu.unbosque.trabajofinalback.dto.GroqRequestDTO;
import co.edu.unbosque.trabajofinalback.dto.GroqResponseDTO;
import co.edu.unbosque.trabajofinalback.exception.AiApiException;
import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.model.Chat;
import co.edu.unbosque.trabajofinalback.model.Message;
import co.edu.unbosque.trabajofinalback.model.MessageType;
import co.edu.unbosque.trabajofinalback.repository.ChatRepository;
import co.edu.unbosque.trabajofinalback.repository.MessageRepository;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Consulta la API de Groq y guarda las conversaciones con la IA. */
@Service
public class AiService {

	private static final String INSTRUCCIONES = "Eres el asistente de un chat llamado WhatsApp Trucho. Responde siempre en español, "
			+ "de forma clara y breve (máximo tres párrafos).";

	private static final int MAX_CARACTERES = 2000;
	private static final int MAX_RESPUESTA = 4000;
	private static final int MENSAJES_CONTEXTO = 10;

	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
			.build();

	@Value("${groq.url}")
	private String url;

	@Value("${groq.api.key:}")
	private String apiKey;

	@Value("${groq.model}")
	private String modelo;

	@Autowired
	private ChatService chatServ;

	@Autowired
	private ChatUnlockService permisos;

	@Autowired
	private ChatRepository chatRepo;

	@Autowired
	private MessageRepository messageRepo;

	@Autowired
	private ChatCryptoService crypto;

	@Autowired
	private AuditService auditService;

	/** Responde una pregunta directa sin consultar ni guardar historial. */
	public String preguntar(String pregunta) throws AiApiException {
		List<GroqMessageDTO> mensajes = new ArrayList<>();
		mensajes.add(new GroqMessageDTO("system", INSTRUCCIONES));
		mensajes.add(new GroqMessageDTO("user", pregunta));
		return consultar(mensajes);
	}

	/**
	 * Consulta a la IA con el historial del chat y guarda la pregunta y la
	 * respuesta.
	 */
	public int askInChat(Long userId, Long chatId, ChatMessageDTO data, String ip) {
		int acceso = chatServ.checkAccess(userId, chatId);
		if (acceso != 0) {
			return acceso;
		}

		if (!permisos.abierto(chatId)) {
			throw new ResponseStatusException(HttpStatus.LOCKED, "Desbloquea el chat para consultar a la IA.");
		}

		String texto = data.getContenido();
		if (texto == null || texto.isBlank()) {
			return 3;
		}

		texto = texto.trim();
		if (texto.length() > MAX_CARACTERES) {
			return 4;
		}

		String respuesta;
		try {
			respuesta = consultar(construirContexto(chatId, texto));
		} catch (AiApiException e) {
			return 5;
		}

		ChatMessageDTO pregunta = new ChatMessageDTO();
		pregunta.setContenido(texto);
		int estado = chatServ.sendMessage(userId, chatId, pregunta, ip);
		if (estado != 0) {
			return estado;
		}

		Chat chat = chatRepo.findById(chatId).orElseThrow();
		String llave = crypto.recuperarLlave(chat.getKeyEnc());
		Message mensaje = new Message(chat, null, MessageType.IA, crypto.cifrar(llave, respuesta), null);
		mensaje = messageRepo.save(mensaje);

		chat.setLastMessageAt(mensaje.getCreatedAt());
		chatRepo.save(chat);

		auditService.log(AuditAction.MESSAGE_SENT, userId, ip,
				"chatId=" + chatId + " messageId=" + mensaje.getId() + " tipo=IA caracteres=" + respuesta.length());

		data.setId(mensaje.getId());
		data.setChatId(chatId);
		data.setRemitenteId(null);
		data.setRemitenteNombre("IA");
		data.setFechaEnvio(mensaje.getCreatedAt().toString());
		data.setMediaUrl(null);

		if (permisos.abierto(chatId)) {
			data.setContenido(respuesta);
			data.setTipo(MessageType.IA.name());
		} else {
			data.setContenido(mensaje.getContentEnc());
			data.setTipo("CIFRADO");
		}
		return 0;
	}

	/** Arma las instrucciones, el historial cronológico y la pregunta para Groq. */
	private List<GroqMessageDTO> construirContexto(Long chatId, String pregunta) {
		List<GroqMessageDTO> mensajes = new ArrayList<>();
		mensajes.add(new GroqMessageDTO("system", INSTRUCCIONES));

		List<ChatMessageDTO> recientes = new ArrayList<>(chatServ.historialParaIa(chatId, MENSAJES_CONTEXTO));
		Collections.reverse(recientes);

		for (ChatMessageDTO mensaje : recientes) {
			String contenido = mensaje.getContenido();
			if (contenido == null || contenido.isBlank()) {
				continue;
			}

			if (MessageType.IA.name().equals(mensaje.getTipo())) {
				mensajes.add(new GroqMessageDTO("assistant", contenido));
			} else {
				mensajes.add(new GroqMessageDTO("user", mensaje.getRemitenteNombre() + ": " + contenido));
			}
		}

		mensajes.add(new GroqMessageDTO("user", pregunta));
		return mensajes;
	}

	/** Comprueba que el backend tenga configurada la clave de Groq. */
	private void validarClaveApi() throws AiApiException {
		if (apiKey == null || apiKey.isBlank()) {
			throw new AiApiException("Falta configurar la clave de la API de IA (GROQ_API_KEY).");
		}
	}

	/** Construye la petición HTTP que se envía a Groq. */
	private HttpRequest crearSolicitudGroq(List<GroqMessageDTO> mensajes) {
		GroqRequestDTO cuerpo = new GroqRequestDTO(modelo, mensajes);
		if (modelo.startsWith("openai/gpt-oss")) {
			cuerpo.setReasoningEffort("low");
		}

		String json = new Gson().toJson(cuerpo);
		return HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
				.uri(URI.create(url)).header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + apiKey).timeout(Duration.ofSeconds(30)).build();
	}

	/** Envía la conversación a Groq y extrae el texto de su respuesta. */
	private String consultar(List<GroqMessageDTO> mensajes) throws AiApiException {
		validarClaveApi();
		HttpRequest request = crearSolicitudGroq(mensajes);
		HttpResponse<String> response;

		try {
			response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (IOException e) {
			throw new AiApiException("No se pudo conectar con la API de IA.");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AiApiException("Se interrumpió la consulta a la IA.");
		}

		if (response.statusCode() == 401) {
			throw new AiApiException("La clave de la API de IA no es válida.");
		}
		if (response.statusCode() == 429) {
			throw new AiApiException("Se alcanzó el límite de la API de IA. Intenta en un momento.");
		}
		if (response.statusCode() != 200) {
			throw new AiApiException();
		}

		GroqResponseDTO respuesta;
		try {
			respuesta = new Gson().fromJson(response.body(), GroqResponseDTO.class);
		} catch (RuntimeException e) {
			throw new AiApiException("La API de IA devolvió una respuesta inesperada.");
		}

		if (respuesta == null || respuesta.getChoices() == null || respuesta.getChoices().isEmpty()
				|| respuesta.getChoices().get(0) == null || respuesta.getChoices().get(0).getMessage() == null) {
			throw new AiApiException("La API de IA devolvió una respuesta inesperada.");
		}

		String contenido = respuesta.getChoices().get(0).getMessage().getContent();
		if (contenido == null || contenido.isBlank()) {
			throw new AiApiException("La IA no generó una respuesta.");
		}

		contenido = contenido.trim();
		if (contenido.length() > MAX_RESPUESTA) {
			contenido = contenido.substring(0, MAX_RESPUESTA);
		}
		return contenido;
	}
}