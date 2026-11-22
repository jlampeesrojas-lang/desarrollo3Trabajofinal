package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.dto.ChatMessageDTO;
import co.edu.unbosque.trabajofinalback.dto.GroqMessageDTO;
import co.edu.unbosque.trabajofinalback.dto.GroqRequestDTO;
import co.edu.unbosque.trabajofinalback.dto.GroqResponseDTO;
import co.edu.unbosque.trabajofinalback.exception.AiApiException;
import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.model.Chat;
import co.edu.unbosque.trabajofinalback.model.Media;
import co.edu.unbosque.trabajofinalback.model.Message;
import co.edu.unbosque.trabajofinalback.model.MessageType;
import co.edu.unbosque.trabajofinalback.repository.ChatRepository;
import co.edu.unbosque.trabajofinalback.repository.MediaRepository;
import co.edu.unbosque.trabajofinalback.repository.MessageRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Asistente de inteligencia artificial de los chats. Usa la API de Groq; la clave nunca sale del
 * back. Entiende texto, imágenes, notas de voz, GIF y archivos de texto. Las respuestas se guardan
 * cifradas como mensajes de tipo IA.
 */
@Service
public class AiService {

  /** Instrucciones que recibe la IA antes de cada conversación. */
	private static final String INSTRUCCIONES =
		      "Eres el asistente de SigmaChat. Tu función principal es ayudar con el uso del chat, es una red social de mensajes"
		          + "pero también puedes responder preguntas generales de forma clara y breve (máx. 3 párrafos) en español. "
		          + "Guía de la app: NO inventes cosas que no esten ene l contexto si no se encuentra  simplemente di que no cuentas con esa informacion"
		          + "1) Tu usuario es con el que creaste la secion si la persona no se acuerda tambien sirve el correo si ninguna de las dos es posible puede preguntar a otro usuario de la app ya que el usuario de cada persona aparece abajo del apodo(El Usuario se usa para añadir contactos, que te añadan y para añadirlos )  "
		          + "2) Añadir contacto: ve al botón/pestaña [ej. Contactos en el panel izquierdo] e ingrésalo por su nombre de usuario exacto (pídeselo a la otra persona). o su correo(Con el cual se registro). no se puede registar con numero "
		          + "3) Cerrar sesión: primero entra a la sección de añadir contactos ubicada arriba de los chats arriba del buscador y pulsa el botón Cerrar sesión ubicado en  la esquina superior derecha solo dice cerrar secion. "
		          + "4) Seguridad: tras 5 intentos fallidos de inicio de sesión, la cuenta se bloquea temporalmente. "
		          + "5) Envíos: mensajes hasta 2000 caracteres; archivos/imágenes/audios máx. 10 MB; usa el botón '+' en la barra inferior del chat para adjuntar, usar cámara o enviar GIFs.";
  /** Longitud máxima de la pregunta. */
  private static final int MAX_CARACTERES = 2000;

  /** Longitud máxima de la respuesta que se guarda. */
  private static final int MAX_RESPUESTA = 4000;

  /** Cantidad de mensajes anteriores del chat que se envían como contexto. */
  private static final int MENSAJES_CONTEXTO = 10;

  /** Tamaño máximo de un archivo de texto que la IA puede leer: 200 KB. */
  private static final int MAX_BYTES_TEXTO = 200 * 1024;

  /** Caracteres máximos del archivo de texto que se envían a la IA. */
  private static final int MAX_CARACTERES_ARCHIVO = 8000;

  /** Bytes del vector de inicialización de los adjuntos subidos desde el chat. */
  private static final int IV_BYTES = 12;

  /** Bits de la etiqueta de autenticación de AES/GCM. */
  private static final int TAG_BITS = 128;

  /** Extensiones de archivo que se leen como texto. */
  private static final List<String> EXTENSIONES_TEXTO =
      List.of("txt", "csv", "md", "xml",
          "log", "properties", "yml", "yaml");

  /** Cliente HTTP compartido, con 10 segundos de espera para conectar. */
  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  /** Dirección de la API de chat (groq.url en application.properties). */
  @Value("${groq.url}")
  private String url;

  /** Clave de la API (secrets.properties o variable de entorno GROQ_API_KEY). */
  @Value("${groq.api.key:}")
  private String apiKey;

  /** Modelo de IA para texto. */
  @Value("${groq.model}")
  private String modelo;

  /** Modelo de IA que entiende imágenes. */
  @Value("${groq.vision-model:qwen/qwen3.8-27b}")
  private String modeloVision;

  /** Dirección de la API que convierte audio en texto. */
  @Value("${groq.audio-url:https://api.groq.com/openai/v1/audio/transcriptions}")
  private String urlAudio;

  /** Modelo que convierte audio en texto. */
  @Value("${groq.audio-model:whisper-large-v3-turbo}")
  private String modeloAudio;

  @Autowired private ChatService chatServ;

  @Autowired private ChatRepository chatRepo;

  @Autowired private MessageRepository messageRepo;

  @Autowired private MediaRepository mediaRepo;

  @Autowired private MediaService mediaServ;

  @Autowired private ChatCryptoService crypto;

  @Autowired private AuditService auditService;

  /** Constructor vacío. */
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
      List<GroqMessageDTO> mensajes = construirHistorial(chatId, null);
      mensajes.add(new GroqMessageDTO("user", texto));
      respuesta = consultar(mensajes);
    } catch (AiApiException e) {
      System.err.println("IA no disponible: " + e.getMessage());
      return 5;
    }

    ChatMessageDTO pregunta = new ChatMessageDTO();
    pregunta.setContenido(texto);
    chatServ.sendMessage(userId, chatId, pregunta, ip);

    Message m = guardarRespuesta(userId, chatId, respuesta, ip);

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
   * Hace que la IA responda al último mensaje del chat, que puede ser un texto, una imagen, una
   * nota de voz, un GIF o un archivo ya enviado. Si el usuario escribió un texto junto con el
   * adjunto, la IA lo tiene en cuenta y el texto se guarda como mensaje del usuario. La respuesta
   * se guarda cifrada como mensaje de tipo IA y se llena en el DTO.
   *
   * @param pregunta texto opcional que el usuario escribió junto con el adjunto
   * @return 0 = respondió, 1 = el chat no existe, 2 = no participa, 3 = no hay un mensaje del
   *     usuario para responder, 4 = el texto supera los 2000 caracteres, 5 = la IA no está
   *     disponible
   */
  public int reply(Long userId, Long chatId, String pregunta, ChatMessageDTO data, String ip) {
    int acceso = chatServ.checkAccess(userId, chatId);
    if (acceso != 0) {
      return acceso;
    }

    String texto = pregunta == null ? "" : pregunta.trim();
    if (texto.length() > MAX_CARACTERES) {
      return 4;
    }

    Optional<Message> encontrado = messageRepo.findFirstByChatIdOrderByIdDesc(chatId);
    if (encontrado.isEmpty() || encontrado.get().getType() == MessageType.IA) {
      return 3;
    }
    Message ultimo = encontrado.get();
    Chat chat = ultimo.getChat();
    String llave = crypto.recuperarLlave(chat.getKeyEnc());
    String contenido = crypto.descifrar(llave, ultimo.getContentEnc());
    String respuesta;

    boolean textoAparte = !texto.isEmpty();
    if (!textoAparte && ultimo.getCaptionEnc() != null) {
      texto = crypto.descifrar(llave, ultimo.getCaptionEnc());
    }
    try {
      List<GroqMessageDTO> mensajes = construirHistorial(chatId, ultimo.getId());
      respuesta = responderSegunTipo(ultimo, contenido, texto, llave, mensajes);
    } catch (AiApiException e) {
      System.err.println("IA no disponible: " + e.getMessage());
      return 5;
    }

    if (textoAparte) {
      ChatMessageDTO escrito = new ChatMessageDTO();
      escrito.setContenido(texto);
      chatServ.sendMessage(userId, chatId, escrito, ip);
    }

    Message m = guardarRespuesta(userId, chatId, respuesta, ip);

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
   * Arma la consulta según el tipo del mensaje y la envía al modelo que corresponde.
   *
   * @param ultimo    mensaje al que se responde
   * @param contenido texto del mensaje ya descifrado (nombre del archivo, URL del GIF o texto)
   * @param pregunta  texto que el usuario escribió junto con el adjunto (puede estar vacío)
   * @param llave     llave del chat
   * @param mensajes  instrucciones e historial previo de la conversación
   * @return la respuesta de la IA
   */
  private String responderSegunTipo(Message ultimo, String contenido, String pregunta,
      String llave, List<GroqMessageDTO> mensajes) throws AiApiException {
    MessageType tipo = ultimo.getType();

    if (tipo == MessageType.GIF) {
      String pedido = conPregunta("El usuario envió este GIF. Reacciona de forma breve.", pregunta);
      try {
        return consultarConImagen(mensajes, pedido, contenido);
      } catch (AiApiException e) {
        mensajes.add(new GroqMessageDTO("user", conPregunta(
            "El usuario envió un GIF. Reacciona de forma breve.", pregunta)));
        return consultar(mensajes);
      }
    }

    if (ultimo.getMediaId() == null) {
      mensajes.add(new GroqMessageDTO("user", conPregunta(contenido, pregunta)));
      return consultar(mensajes);
    }

    Optional<Media> archivo = mediaRepo.findById(ultimo.getMediaId());
    if (archivo.isEmpty()) {
      throw new AiApiException("El archivo del mensaje ya no existe.");
    }
    Media media = archivo.get();
    byte[] datos = leerAdjunto(media, llave);
    String mime = media.getMimeType();

    if (tipo == MessageType.IMAGEN) {
      String imagen = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(datos);
      return consultarConImagen(mensajes, conPregunta(
          "El usuario envió esta imagen. Descríbela y responde lo que pregunte.", pregunta), imagen);
    }

    if (tipo == MessageType.AUDIO) {
      String transcripcion = transcribir(datos, mime);
      mensajes.add(new GroqMessageDTO("user", conPregunta(
          "El usuario envió una nota de voz que dice: \"" + transcripcion + "\". Respóndele.",
          pregunta)));
      return consultar(mensajes);
    }

    String nombre = contenido == null ? "archivo" : contenido;
    if (esTexto(nombre) && datos.length <= MAX_BYTES_TEXTO) {
      String texto = new String(datos, StandardCharsets.UTF_8);
      if (texto.length() > MAX_CARACTERES_ARCHIVO) {
        texto = texto.substring(0, MAX_CARACTERES_ARCHIVO);
      }
      mensajes.add(new GroqMessageDTO("user", conPregunta(
          "El usuario envió el archivo \"" + nombre + "\" con este contenido:\n" + texto
              + "\nResúmelo y responde lo que pregunte.", pregunta)));
      return consultar(mensajes);
    }

    mensajes.add(new GroqMessageDTO("user", conPregunta(
        "El usuario envió el archivo \"" + nombre + "\" (" + mime + "). No puedes leer su"
            + " contenido; dile qué tipos de archivo sí puedes revisar: imágenes, notas de voz y"
            + " archivos de texto.", pregunta)));
    return consultar(mensajes);
  }

  /**
   * Agrega al pedido el texto que escribió el usuario junto con el adjunto.
   *
   * @param pedido   instrucción para la IA según el tipo de adjunto
   * @param pregunta texto del usuario (puede estar vacío)
   * @return el pedido con el texto del usuario al final, si lo hay
   */
  private String conPregunta(String pedido, String pregunta) {
    if (pregunta == null || pregunta.isBlank()) {
      return pedido;
    }
    return pedido + " Junto con el adjunto, el usuario escribió: \"" + pregunta + "\".";
  }

  /** Guarda la respuesta de la IA como mensaje cifrado y la deja en la auditoría. */
  private Message guardarRespuesta(Long userId, Long chatId, String respuesta, String ip) {
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
    return m;
  }

  /**
   * Arma las instrucciones y los últimos mensajes del chat para darle contexto a la IA. Los
   * mensajes eliminados y los que llegan cifrados no se envían como contexto.
   *
   * @param chatId   identificador del chat
   * @param excluido id de un mensaje que no se incluye (el que se va a responder), o null
   * @return lista de mensajes lista para agregar la consulta nueva
   */
  private List<GroqMessageDTO> construirHistorial(Long chatId, Long excluido) {
    List<GroqMessageDTO> mensajes = new ArrayList<>();
    mensajes.add(new GroqMessageDTO("system", INSTRUCCIONES));

    List<ChatMessageDTO> recientes =
        new ArrayList<>(chatServ.getMessages(chatId, 0, MENSAJES_CONTEXTO));
    Collections.reverse(recientes);

    for (ChatMessageDTO m : recientes) {
      if (m.getContenido() == null || m.getContenido().isBlank() || m.getId().equals(excluido)
          || "ELIMINADO".equals(m.getTipo()) || "CIFRADO".equals(m.getTipo())) {
        continue;
      }
      if (MessageType.IA.name().equals(m.getTipo())) {
        mensajes.add(new GroqMessageDTO("assistant", m.getContenido()));
      } else if (MessageType.TEXTO.name().equals(m.getTipo())) {
        mensajes.add(new GroqMessageDTO("user", m.getRemitenteNombre() + ": " + m.getContenido()));
      } else {
          String leyenda = m.getLeyenda() == null ? "" : " con el texto \"" + m.getLeyenda() + "\"";
          mensajes.add(new GroqMessageDTO("user",
              m.getRemitenteNombre() + " envió un adjunto de tipo " + m.getTipo() + leyenda + "."));
        }
    }
    return mensajes;
  }

  /** Indica si el archivo se puede leer como texto según su extensión. */
  private boolean esTexto(String nombre) {
    int punto = nombre.lastIndexOf('.');
    if (punto < 0) {
      return false;
    }
    String extension = nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    return EXTENSIONES_TEXTO.contains(extension);
  }

  /**
   * Recupera los bytes originales de un adjunto. Los adjuntos subidos desde la barra del chat
   * llevan el vector de inicialización al inicio; los subidos por la otra ruta usan el formato de
   * MediaService.
   */
  private byte[] leerAdjunto(Media media, String llave) throws AiApiException {
    byte[] guardado = media.getDataEnc();
    try {
      if (guardado != null && guardado.length > IV_BYTES + TAG_BITS / 8) {
        ByteBuffer buffer = ByteBuffer.wrap(guardado);
        byte[] iv = new byte[IV_BYTES];
        buffer.get(iv);
        byte[] cifrado = new byte[buffer.remaining()];
        buffer.get(cifrado);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec clave = new SecretKeySpec(llave.getBytes(StandardCharsets.UTF_8), "AES");
        cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
        return cipher.doFinal(cifrado);
      }
    } catch (GeneralSecurityException e) {
    }
    try {
      return mediaServ.descifrar(media);
    } catch (RuntimeException e) {
      throw new AiApiException("No se pudo leer el archivo para la IA.");
    }
  }

  /**
   * Consulta al modelo de visión con una imagen.
   *
   * @param mensajes instrucciones e historial previo
   * @param pedido   texto que acompaña la imagen
   * @param imagen   URL pública o URL de datos en Base64
   * @return la respuesta de la IA
   */
  private String consultarConImagen(List<GroqMessageDTO> mensajes, String pedido, String imagen)
      throws AiApiException {
    Gson gson = new Gson();
    JsonArray lista = gson.toJsonTree(mensajes).getAsJsonArray();

    JsonObject parteTexto = new JsonObject();
    parteTexto.addProperty("type", "text");
    parteTexto.addProperty("text", pedido);

    JsonObject direccion = new JsonObject();
    direccion.addProperty("url", imagen);
    JsonObject parteImagen = new JsonObject();
    parteImagen.addProperty("type", "image_url");
    parteImagen.add("image_url", direccion);

    JsonArray partes = new JsonArray();
    partes.add(parteTexto);
    partes.add(parteImagen);

    JsonObject usuario = new JsonObject();
    usuario.addProperty("role", "user");
    usuario.add("content", partes);
    lista.add(usuario);

    JsonObject cuerpo = new JsonObject();
    cuerpo.addProperty("model", modeloVision);
    cuerpo.add("messages", lista);
    cuerpo.addProperty("temperature", 0.7);
    cuerpo.addProperty("max_completion_tokens", 1024);

    return enviarConsulta(gson.toJson(cuerpo));
  }

  /**
   * Convierte una nota de voz en texto con el modelo de audio de Groq.
   *
   * @param datos bytes del audio
   * @param mime  tipo del audio
   * @return el texto de la nota de voz
   */
  private String transcribir(byte[] datos, String mime) throws AiApiException {
    validarClaveApi();

    String limite = "----WhatsAppTrucho" + System.currentTimeMillis();
    ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();
    escribirCampo(cuerpo, limite, "model", modeloAudio);
    escribirCampo(cuerpo, limite, "language", "es");
    escribirCampo(cuerpo, limite, "response_format", "json");
    cuerpo.writeBytes(("--" + limite + "\r\n"
        + "Content-Disposition: form-data; name=\"file\"; filename=\"nota" + extensionAudio(mime)
        + "\"\r\n" + "Content-Type: " + mime + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
    cuerpo.writeBytes(datos);
    cuerpo.writeBytes(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));

    HttpRequest request =
        HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo.toByteArray()))
            .uri(URI.create(urlAudio))
            .header("Content-Type", "multipart/form-data; boundary=" + limite)
            .header("Authorization", "Bearer " + apiKey)
            .timeout(Duration.ofSeconds(60))
            .build();

    HttpResponse<String> response = enviar(request);
    try {
      JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
      JsonElement texto = json.get("text");
      if (texto == null || texto.getAsString().isBlank()) {
        throw new AiApiException("No se entendió la nota de voz.");
      }
      return texto.getAsString().trim();
    } catch (RuntimeException e) {
      throw new AiApiException("La API de audio devolvió una respuesta inesperada.");
    }
  }

  /** Escribe un campo de texto en un cuerpo multipart. */
  private void escribirCampo(ByteArrayOutputStream cuerpo, String limite, String nombre,
      String valor) {
    cuerpo.writeBytes(("--" + limite + "\r\n"
        + "Content-Disposition: form-data; name=\"" + nombre + "\"\r\n\r\n"
        + valor + "\r\n").getBytes(StandardCharsets.UTF_8));
  }

  /** Extensión de archivo que corresponde al tipo de audio. */
  private String extensionAudio(String mime) {
    if (mime.contains("webm")) {
      return ".webm";
    }
    if (mime.contains("ogg")) {
      return ".ogg";
    }
    if (mime.contains("wav")) {
      return ".wav";
    }
    if (mime.contains("mp4")) {
      return ".m4a";
    }
    return ".mp3";
  }

  /** Revisa que la clave de la API de Groq esté configurada. */
  private void validarClaveApi() throws AiApiException {
    if (apiKey == null || apiKey.isBlank()) {
      throw new AiApiException("Falta configurar la clave de la API de IA (GROQ_API_KEY).");
    }
  }

  /**
   * Envía una conversación de solo texto al modelo de texto y devuelve la respuesta.
   *
   * @throws AiApiException si no hay clave, la API no responde, responde con error o devuelve algo
   *     inesperado
   */
  private String consultar(List<GroqMessageDTO> mensajes) throws AiApiException {
    GroqRequestDTO cuerpo = new GroqRequestDTO(modelo, mensajes);
    if (modelo.startsWith("openai/gpt-oss")) {
      cuerpo.setReasoningEffort("low");
    }
    return enviarConsulta(new Gson().toJson(cuerpo));
  }

  /**
   * Envía un cuerpo JSON a la API de chat de Groq y devuelve el texto de la respuesta.
   *
   * @param json cuerpo de la petición
   * @return la respuesta de la IA, sin el razonamiento interno y con el largo máximo permitido
   */
  private String enviarConsulta(String json) throws AiApiException {
    validarClaveApi();

    HttpRequest request =
        HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .timeout(Duration.ofSeconds(60))
            .build();

    HttpResponse<String> response = enviar(request);

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
    if (contenido == null) {
      throw new AiApiException("La IA no generó una respuesta.");
    }
    contenido = contenido.replaceAll("(?s)<think>.*?</think>", "").trim();
    if (contenido.isBlank()) {
      throw new AiApiException("La IA no generó una respuesta.");
    }
    if (contenido.length() > MAX_RESPUESTA) {
      contenido = contenido.substring(0, MAX_RESPUESTA);
    }
    return contenido;
  }

  /**
   * Envía una petición a Groq y revisa el código de respuesta.
   *
   * @return la respuesta con código 200
   * @throws AiApiException si no hubo respuesta o Groq respondió con error
   */
  private HttpResponse<String> enviar(HttpRequest request) throws AiApiException {
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
    return response;
  }
}