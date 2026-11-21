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
import org.springframework.stereotype.Service;

/**
 * Asistente de inteligencia artificial de los chats. Usa la API de Groq; la clave nunca sale del
 * back. Las respuestas se guardan cifradas como mensajes de tipo IA.
 */
@Service
public class AiService {

  /** Instrucciones que recibe la IA antes de cada conversación. */
  private static final String INSTRUCCIONES =
      "Eres el asistente de un chat llamado WhatsApp Trucho. Responde siempre en español, de"
          + " forma clara y breve (máximo tres párrafos).";

  /** Longitud máxima de la pregunta. */
  private static final int MAX_CARACTERES = 2000;

  /** Longitud máxima de la respuesta que se guarda. */
  private static final int MAX_RESPUESTA = 4000;

  /** Cantidad de mensajes anteriores del chat que se envían como contexto. */
  private static final int MENSAJES_CONTEXTO = 10;

  /** Cliente HTTP compartido, con 10 segundos de espera para conectar. */
  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  /** Dirección de la API (groq.url en application.properties). */
  @Value("${groq.url}")
  private String url;

  /** Clave de la API (variable de entorno GROQ_API_KEY). */
  @Value("${groq.api.key:}")
  private String apiKey;

  /** Modelo de IA que se usa (variable de entorno GROQ_MODEL o el valor por defecto). */
  @Value("${groq.model}")
  private String modelo;

  @Autowired private ChatService chatServ;

  @Autowired private ChatRepository chatRepo;

  @Autowired private MessageRepository messageRepo;

  @Autowired private ChatCryptoService crypto;

  @Autowired private AuditService auditService;

  public AiService() {}

  /**
   * Hace una pregunta directa a la IA, sin guardar nada.
   *
   * @param pregunta texto de la pregunta
   * @return la respuesta de la IA
   * @throws AiApiException si la API falla o no hay clave configurada
   */
  public String preguntar(String pregunta) throws AiApiException {
    List<GroqMessageDTO> mensajes = new ArrayList<>();
    mensajes.add(new GroqMessageDTO("system", INSTRUCCIONES));
    mensajes.add(new GroqMessageDTO("user", pregunta));
    return consultar(mensajes);
  }

  /**
   * Pregunta a la IA dentro de un chat. Guarda la pregunta como mensaje de texto del usuario y la
   * respuesta como mensaje de tipo IA, ambas cifradas. Llena los datos de la respuesta en el mismo
   * DTO.
   *
   * @return 0 = respondió, 1 = el chat no existe, 2 = no participa, 3 = pregunta vacía, 4 = más de
   *     2000 caracteres, 5 = la IA no está disponible
   */
  public int askInChat(Long userId, Long chatId, ChatMessageDTO data, String ip) {
    int acceso = chatServ.checkAccess(userId, chatId);
    if (acceso != 0) {
      return acceso;
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
      System.err.println("IA no disponible: " + e.getMessage());
      return 5;
    }

    ChatMessageDTO pregunta = new ChatMessageDTO();
    pregunta.setContenido(texto);
    chatServ.sendMessage(userId, chatId, pregunta, ip);

    Chat chat = chatRepo.findById(chatId).get();
    String llave = crypto.recuperarLlave(chat.getKeyEnc());
    Message m =
        messageRepo.save(
            new Message(chat, null, MessageType.IA, crypto.cifrar(llave, respuesta), null));
    chat.setLastMessageAt(m.getCreatedAt());
    chatRepo.save(chat);

    auditService.log(
        AuditAction.MESSAGE_SENT,
        userId,
        ip,
        "chatId=" + chatId + " messageId=" + m.getId() + " tipo=IA caracteres=" + respuesta.length());

    data.setId(m.getId());
    data.setChatId(chatId);
    data.setRemitenteId(null);
    data.setRemitenteNombre("IA");
    data.setContenido(respuesta);
    data.setTipo(MessageType.IA.name());
    data.setFechaEnvio(m.getCreatedAt().toString());
    return 0;
  }

  /**
   * Arma la conversación que se envía a la IA: las instrucciones, los últimos mensajes del chat y
   * la pregunta nueva.
   */
  private List<GroqMessageDTO> construirContexto(Long chatId, String pregunta) {
    List<GroqMessageDTO> mensajes = new ArrayList<>();
    mensajes.add(new GroqMessageDTO("system", INSTRUCCIONES));

    List<ChatMessageDTO> recientes =
        new ArrayList<>(chatServ.getMessages(chatId, 0, MENSAJES_CONTEXTO));
    Collections.reverse(recientes);

    for (ChatMessageDTO m : recientes) {
      if (m.getContenido() == null || m.getContenido().isBlank()) {
        continue;
      }
      if (MessageType.IA.name().equals(m.getTipo())) {
        mensajes.add(new GroqMessageDTO("assistant", m.getContenido()));
      } else {
        mensajes.add(new GroqMessageDTO("user", m.getRemitenteNombre() + ": " + m.getContenido()));
      }
    }

    mensajes.add(new GroqMessageDTO("user", pregunta));
    return mensajes;
  }

  private void validarClaveApi() throws AiApiException {
    if (apiKey == null || apiKey.isBlank()) {
      throw new AiApiException("Falta configurar la clave de la API de IA (GROQ_API_KEY).");
    }
  }
  private HttpRequest crearSolicitudGroq(List<GroqMessageDTO> mensajes) {
    GroqRequestDTO cuerpo = new GroqRequestDTO(modelo, mensajes);
    if (modelo.startsWith("openai/gpt-oss")) {
      cuerpo.setReasoningEffort("low");
    }
    String json = new Gson().toJson(cuerpo);

    return HttpRequest.newBuilder()
        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + apiKey)
        .timeout(Duration.ofSeconds(30))
        .build();
  }
  /**
   * Envía la conversación a la API de Groq y devuelve el texto de la respuesta.
   *
   * @throws AiApiException si no hay clave, la API no responde, responde con error o devuelve algo
   *     inesperado
   */
  private String consultar(List<GroqMessageDTO> mensajes) throws AiApiException {
    validarClaveApi();

    HttpRequest request = crearSolicitudGroq(mensajes);

    HttpResponse<String> response = null;
    try {
      response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      e.printStackTrace();
    }

    if (response == null) {
      throw new AiApiException();
    }
    if (response.statusCode() == 401) {
      throw new AiApiException("La clave de la API de IA no es válida.");
    }
    if (response.statusCode() == 429) {
      throw new AiApiException("Se alcanzó el límite gratuito de la IA. Intenta en un momento.");
    }
    if (response.statusCode() != 200) {
      System.err.println("Groq respondió " + response.statusCode() + ": " + response.body());
      throw new AiApiException();
    }

    GroqResponseDTO respuesta = null;
    try {
      respuesta = new Gson().fromJson(response.body(), GroqResponseDTO.class);
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (respuesta == null
        || respuesta.getChoices() == null
        || respuesta.getChoices().isEmpty()
        || respuesta.getChoices().get(0).getMessage() == null) {
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