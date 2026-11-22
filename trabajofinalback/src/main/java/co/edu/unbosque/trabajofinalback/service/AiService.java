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
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Atiende las consultas de Sigma Chat y guarda las respuestas cifradas. */
@Service
public class AiService {

    private static final String INSTRUCCIONES =
            "Eres el asistente de Sigma Chat. Ayudas con el uso de la aplicación "
            + "y también respondes preguntas generales. Responde en español, "
            + "de forma clara y breve, en un máximo de tres párrafos. "
            + "Guía de la aplicación: "
            + "1) Para iniciar sesión se usa el nombre de usuario o el correo registrado. "
            + "Si alguien olvida su usuario, uno de sus contactos puede verlo debajo del apodo. "
            + "2) Para añadir un contacto, pulsa '+ Añadir contactos', encima del buscador, "
            + "e introduce su nombre de usuario exacto o su correo registrado. "
            + "3) Para cerrar sesión, entra en Añadir contactos y pulsa Cerrar sesión "
            + "en la esquina superior derecha. "
            + "4) Tras cinco intentos fallidos de inicio de sesión, la cuenta se bloquea temporalmente. "
            + "5) Los mensajes admiten hasta 2000 caracteres y los adjuntos del chat hasta 10 MB. "
            + "El botón '+' permite elegir archivo, cámara o GIF. "
            + "El micrófono permite grabar una nota de voz y acompañarla con texto. "
            + "6) Para abrir una conversación bloqueada, pulsa Desbloquear e ingresa la clave del chat. "
            + "Ver clave solicita la contraseña de la cuenta. Cada participante desbloquea "
            + "su propia vista de la conversación.";

    private static final int MAX_CARACTERES = 2000;
    private static final int MAX_RESPUESTA = 4000;
    private static final int MENSAJES_CONTEXTO = 10;
    private static final int MAX_BYTES_TEXTO = 200 * 1024;
    private static final int MAX_CARACTERES_ARCHIVO = 8000;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private static final List<String> EXTENSIONES_TEXTO =
            List.of("txt", "csv", "md", "xml", "log", "properties", "yml", "yaml");

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Value("${groq.url}")
    private String url;

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model}")
    private String modelo;

    @Value("${groq.vision-model:qwen/qwen3.8-27b}")
    private String modeloVision;

    @Value("${groq.audio-url:https://api.groq.com/openai/v1/audio/transcriptions}")
    private String urlAudio;

    @Value("${groq.audio-model:whisper-large-v3-turbo}")
    private String modeloAudio;

    @Autowired
    private ChatService chatServ;

    @Autowired
    private ChatUnlockService permisos;

    @Autowired
    private ChatRepository chatRepo;

    @Autowired
    private MessageRepository messageRepo;

    @Autowired
    private MediaRepository mediaRepo;

    @Autowired
    private MediaService mediaServ;

    @Autowired
    private ChatCryptoService crypto;

    @Autowired
    private AuditService auditService;

    /** Responde una pregunta directa sin consultar el historial de un chat. */
    public String preguntar(String pregunta) throws AiApiException {
        List<GroqMessageDTO> mensajes = new ArrayList<>();
        mensajes.add(new GroqMessageDTO("system", INSTRUCCIONES));
        mensajes.add(new GroqMessageDTO("user", pregunta));
        return consultar(mensajes);
    }

    /** Responde una pregunta de texto y guarda la pregunta y la respuesta en el chat. */
    public int askInChat(Long userId, Long chatId, ChatMessageDTO data, String ip) {
        int acceso = chatServ.checkAccess(userId, chatId);
        if (acceso != 0) {
            return acceso;
        }
        exigirDesbloqueo(chatId);

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
            return 5;
        }

        ChatMessageDTO pregunta = new ChatMessageDTO();
        pregunta.setContenido(texto);

        int estado = chatServ.sendMessage(userId, chatId, pregunta, ip);
        if (estado != 0) {
            return estado;
        }

        Message mensaje = guardarRespuesta(userId, chatId, respuesta, ip);
        completarRespuesta(data, mensaje, respuesta);
        return 0;
    }

    /** Responde al último mensaje o adjunto y tiene en cuenta su leyenda. */
    public int reply(Long userId, Long chatId, String pregunta, ChatMessageDTO data, String ip) {
        int acceso = chatServ.checkAccess(userId, chatId);
        if (acceso != 0) {
            return acceso;
        }
        exigirDesbloqueo(chatId);

        String texto = pregunta == null ? "" : pregunta.trim();
        if (texto.length() > MAX_CARACTERES) {
            return 4;
        }

        Optional<Message> encontrado = messageRepo.findFirstByChatIdOrderByIdDesc(chatId);
        if (encontrado.isEmpty() || encontrado.get().getType() == MessageType.IA) {
            return 3;
        }

        Message ultimo = encontrado.get();
        String llave = crypto.recuperarLlave(ultimo.getChat().getKeyEnc());
        String contenido = crypto.descifrar(llave, ultimo.getContentEnc());

        // Una leyenda guardada en el adjunto ya aparece en esa misma burbuja.
        boolean textoAparte = !texto.isEmpty();
        if (!textoAparte && ultimo.getCaptionEnc() != null) {
            texto = crypto.descifrar(llave, ultimo.getCaptionEnc());
        }

        String respuesta;
        try {
            List<GroqMessageDTO> mensajes = construirHistorial(chatId, ultimo.getId());
            respuesta = responderSegunTipo(ultimo, contenido, texto, llave, mensajes);
        } catch (AiApiException e) {
            return 5;
        }

        if (textoAparte) {
            ChatMessageDTO escrito = new ChatMessageDTO();
            escrito.setContenido(texto);

            int estado = chatServ.sendMessage(userId, chatId, escrito, ip);
            if (estado != 0) {
                return estado;
            }
        }

        Message mensaje = guardarRespuesta(userId, chatId, respuesta, ip);
        completarRespuesta(data, mensaje, respuesta);
        return 0;
    }

    /** Comprueba el permiso antes de leer el historial o un archivo para la IA. */
    private void exigirDesbloqueo(Long chatId) {
        if (!permisos.abierto(chatId)) {
            throw new ResponseStatusException(
                    HttpStatus.LOCKED, "Desbloquea el chat para consultar a la IA.");
        }
    }

    /** Comprueba de nuevo el permiso al terminar la llamada a Groq. */
    private void completarRespuesta(ChatMessageDTO data, Message mensaje, String respuesta) {
        Long chatId = mensaje.getChat().getId();

        data.setId(mensaje.getId());
        data.setChatId(chatId);
        data.setRemitenteId(null);
        data.setRemitenteNombre("IA");
        data.setFechaEnvio(mensaje.getCreatedAt().toString());
        data.setMediaUrl(null);
        data.setLeyenda(null);

        if (permisos.abierto(chatId)) {
            data.setContenido(respuesta);
            data.setTipo(MessageType.IA.name());
        } else {
            data.setContenido(mensaje.getContentEnc());
            data.setTipo("CIFRADO");
        }
    }

    /** Guarda la respuesta cifrada y registra el envío en la auditoría. */
    private Message guardarRespuesta(Long userId, Long chatId, String respuesta, String ip) {
        Chat chat = chatRepo.findById(chatId).orElseThrow();
        String llave = crypto.recuperarLlave(chat.getKeyEnc());

        Message mensaje = new Message(
                chat, null, MessageType.IA, crypto.cifrar(llave, respuesta), null);
        mensaje = messageRepo.save(mensaje);

        chat.setLastMessageAt(mensaje.getCreatedAt());
        chatRepo.save(chat);

        auditService.log(AuditAction.MESSAGE_SENT, userId, ip,
                "chatId=" + chatId + " messageId=" + mensaje.getId()
                        + " tipo=IA caracteres=" + respuesta.length());

        return mensaje;
    }

    /** Ordena los últimos mensajes del más antiguo al más reciente para Groq. */
    private List<GroqMessageDTO> construirHistorial(Long chatId, Long excluido) {
        List<GroqMessageDTO> mensajes = new ArrayList<>();
        mensajes.add(new GroqMessageDTO("system", INSTRUCCIONES));

        List<ChatMessageDTO> recientes =
                new ArrayList<>(chatServ.historialParaIa(chatId, MENSAJES_CONTEXTO));
        Collections.reverse(recientes);

        for (ChatMessageDTO mensaje : recientes) {
            if (mensaje.getId().equals(excluido)) {
                continue;
            }

            String contenido = mensaje.getContenido();
            if (contenido == null || contenido.isBlank()) {
                continue;
            }

            if (MessageType.IA.name().equals(mensaje.getTipo())) {
                mensajes.add(new GroqMessageDTO("assistant", contenido));
            } else if (MessageType.TEXTO.name().equals(mensaje.getTipo())) {
                mensajes.add(new GroqMessageDTO(
                        "user", mensaje.getRemitenteNombre() + ": " + contenido));
            } else {
                String descripcion = mensaje.getRemitenteNombre()
                        + " envió un adjunto de tipo " + mensaje.getTipo();

                if (mensaje.getLeyenda() != null && !mensaje.getLeyenda().isBlank()) {
                    descripcion += " con el texto \"" + mensaje.getLeyenda() + "\"";
                }
                mensajes.add(new GroqMessageDTO("user", descripcion + "."));
            }
        }
        return mensajes;
    }

    /** Prepara la consulta según el tipo del último mensaje. */
    private String responderSegunTipo(
            Message ultimo, String contenido, String pregunta, String llave,
            List<GroqMessageDTO> mensajes) throws AiApiException {

        MessageType tipo = ultimo.getType();

        if (tipo == MessageType.GIF) {
            String pedido = conPregunta("El usuario envió este GIF. Reacciona de forma breve.", pregunta);
            try {
                return consultarConImagen(mensajes, pedido, contenido);
            } catch (AiApiException e) {
                mensajes.add(new GroqMessageDTO("user", conPregunta(
                        "El usuario envió un GIF que no pudiste visualizar. Respóndele brevemente.",
                        pregunta)));
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
        String mime = media.getMimeType();

        if (tipo == MessageType.IMAGEN) {
            byte[] datos = leerAdjunto(media, llave);
            String imagen = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(datos);
            String pedido = conPregunta(
                    "El usuario envió esta imagen. Descríbela y responde lo que pregunte.", pregunta);
            return consultarConImagen(mensajes, pedido, imagen);
        }

        if (tipo == MessageType.AUDIO) {
            byte[] datos = leerAdjunto(media, llave);
            String transcripcion = transcribir(datos, mime);
            String pedido = "El usuario envió una nota de voz que dice: \""
                    + transcripcion + "\". Respóndele.";
            mensajes.add(new GroqMessageDTO("user", conPregunta(pedido, pregunta)));
            return consultar(mensajes);
        }

        String nombre = contenido == null ? "archivo" : contenido;
        if (esTexto(nombre)) {
            byte[] datos = leerAdjunto(media, llave);
            if (datos.length <= MAX_BYTES_TEXTO) {
                String textoArchivo = new String(datos, StandardCharsets.UTF_8);
                if (textoArchivo.length() > MAX_CARACTERES_ARCHIVO) {
                    textoArchivo = textoArchivo.substring(0, MAX_CARACTERES_ARCHIVO);
                }

                String pedido = "El usuario envió el archivo \"" + nombre
                        + "\" con este contenido:\n" + textoArchivo
                        + "\nResúmelo y responde lo que pregunte.";
                mensajes.add(new GroqMessageDTO("user", conPregunta(pedido, pregunta)));
                return consultar(mensajes);
            }
        }

        String pedido = "El usuario envió el archivo \"" + nombre + "\" (" + mime
                + "). Explica que puedes revisar imágenes, notas de voz y archivos de texto "
                + "compatibles de hasta 200 KB.";
        mensajes.add(new GroqMessageDTO("user", conPregunta(pedido, pregunta)));
        return consultar(mensajes);
    }

    /** Añade a la consulta la instrucción que acompaña al adjunto. */
    private String conPregunta(String pedido, String pregunta) {
        if (pregunta == null || pregunta.isBlank()) {
            return pedido;
        }
        return pedido + " Junto con el adjunto, el usuario escribió: \"" + pregunta + "\".";
    }

    /** Reconoce las extensiones de los archivos que se pueden leer como texto. */
    private boolean esTexto(String nombre) {
        int punto = nombre.lastIndexOf('.');
        if (punto < 0) {
            return false;
        }

        String extension = nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
        return EXTENSIONES_TEXTO.contains(extension);
    }

    /** Recupera los bytes de un adjunto creado por cualquiera de las dos rutas de subida. */
    private byte[] leerAdjunto(Media media, String llave) throws AiApiException {
        byte[] guardado = media.getDataEnc();

        if (guardado != null && guardado.length >= IV_BYTES + TAG_BITS / 8) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(guardado);
                byte[] iv = new byte[IV_BYTES];
                buffer.get(iv);

                byte[] cifrado = new byte[buffer.remaining()];
                buffer.get(cifrado);

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                SecretKeySpec clave = new SecretKeySpec(
                        llave.getBytes(StandardCharsets.UTF_8), "AES");

                cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
                return cipher.doFinal(cifrado);
            } catch (GeneralSecurityException e) {
                // La ruta /media/upload usa el formato que descifra MediaService.
            }
        }

        try {
            return mediaServ.descifrar(media);
        } catch (RuntimeException e) {
            throw new AiApiException("No se pudo leer el archivo para la IA.");
        }
    }

    /** Envía al modelo de visión la imagen junto con el historial y la pregunta. */
    private String consultarConImagen(
            List<GroqMessageDTO> mensajes, String pedido, String imagen) throws AiApiException {

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

    /** Envía una nota de voz a Groq y recupera su transcripción en español. */
    private String transcribir(byte[] datos, String mime) throws AiApiException {
        validarClaveApi();

        String limite = "----SigmaChat" + UUID.randomUUID();
        ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();

        escribirCampo(cuerpo, limite, "model", modeloAudio);
        escribirCampo(cuerpo, limite, "language", "es");
        escribirCampo(cuerpo, limite, "response_format", "json");

        String cabeceraArchivo = "--" + limite + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"nota"
                + extensionAudio(mime) + "\"\r\n"
                + "Content-Type: " + mime + "\r\n\r\n";

        cuerpo.writeBytes(cabeceraArchivo.getBytes(StandardCharsets.UTF_8));
        cuerpo.writeBytes(datos);
        cuerpo.writeBytes(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlAudio))
                .header("Content-Type", "multipart/form-data; boundary=" + limite)
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo.toByteArray()))
                .build();

        HttpResponse<String> response = enviar(request);

        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonElement texto = json.get("text");

            if (texto == null || texto.isJsonNull() || texto.getAsString().isBlank()) {
                throw new AiApiException("No se entendió la nota de voz.");
            }
            return texto.getAsString().trim();
        } catch (RuntimeException e) {
            throw new AiApiException("La API de audio devolvió una respuesta inesperada.");
        }
    }

    /** Escribe un campo de texto en el cuerpo multipart de la transcripción. */
    private void escribirCampo(
            ByteArrayOutputStream cuerpo, String limite, String nombre, String valor) {

        String campo = "--" + limite + "\r\n"
                + "Content-Disposition: form-data; name=\"" + nombre + "\"\r\n\r\n"
                + valor + "\r\n";

        cuerpo.writeBytes(campo.getBytes(StandardCharsets.UTF_8));
    }

    /** Elige la extensión del archivo que se envía al servicio de transcripción. */
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
        if (mime.contains("mp4") || mime.contains("m4a")) {
            return ".m4a";
        }
        return ".mp3";
    }

    /** Comprueba que el backend tenga configurada la clave de Groq. */
    private void validarClaveApi() throws AiApiException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiApiException("Falta configurar la clave de la API de IA (GROQ_API_KEY).");
        }
    }

    /** Envía el historial y la pregunta al modelo de texto. */
    private String consultar(List<GroqMessageDTO> mensajes) throws AiApiException {
        GroqRequestDTO cuerpo = new GroqRequestDTO(modelo, mensajes);

        if (modelo.startsWith("openai/gpt-oss")) {
            cuerpo.setReasoningEffort("low");
        }

        return enviarConsulta(new Gson().toJson(cuerpo));
    }

    /** Envía una consulta de chat y obtiene el texto de la respuesta. */
    private String enviarConsulta(String json) throws AiApiException {
        validarClaveApi();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = enviar(request);
        GroqResponseDTO respuesta;

        try {
            respuesta = new Gson().fromJson(response.body(), GroqResponseDTO.class);
        } catch (RuntimeException e) {
            throw new AiApiException("La API de IA devolvió una respuesta inesperada.");
        }

        if (respuesta == null
                || respuesta.getChoices() == null
                || respuesta.getChoices().isEmpty()
                || respuesta.getChoices().get(0) == null
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

    /** Ejecuta una petición a Groq y comprueba su resultado. */
    private HttpResponse<String> enviar(HttpRequest request) throws AiApiException {
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
            throw new AiApiException("Se alcanzó el límite de la API de IA. Intenta más tarde.");
        }
        if (response.statusCode() != 200) {
            throw new AiApiException("La API de IA respondió con HTTP " + response.statusCode() + ".");
        }
        return response;
    }
}