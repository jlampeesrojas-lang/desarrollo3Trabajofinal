package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.model.Chat;
import co.edu.unbosque.trabajofinalback.model.ChatParticipant;
import co.edu.unbosque.trabajofinalback.model.Media;
import co.edu.unbosque.trabajofinalback.model.Message;
import co.edu.unbosque.trabajofinalback.model.MessageType;
import co.edu.unbosque.trabajofinalback.repository.ChatParticipantRepository;
import co.edu.unbosque.trabajofinalback.repository.ChatRepository;
import co.edu.unbosque.trabajofinalback.repository.MediaRepository;
import co.edu.unbosque.trabajofinalback.repository.MessageRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Subida y descarga de imágenes y audios de los chats. */
@Service
public class MediaService {

  /** Tamaño máximo de una imagen: 5 MB. */
  private static final long MAX_IMAGEN = 5L * 1024 * 1024;

  /** Tamaño máximo de un audio: 10 MB. */
  private static final long MAX_AUDIO = 10L * 1024 * 1024;

  /** Longitud máxima del texto que acompaña al archivo. */
  private static final int MAX_CARACTERES = 2000;

  @Autowired private ChatService chatServ;

  @Autowired private ChatRepository chatRepo;

  @Autowired private ChatParticipantRepository participantRepo;

  @Autowired private MediaRepository mediaRepo;

  @Autowired private MessageRepository messageRepo;

  @Autowired private ChatCryptoService crypto;

  @Autowired private AuditService auditService;

  /** Constructor vacío. */
  public MediaService() {}

  /**
   * Valida, cifra y guarda un archivo, y crea el mensaje de tipo IMAGEN o AUDIO en el chat.
   *
   * @param contenido texto opcional que acompaña al archivo
   * @return 0 = enviado, 1 = el chat no existe, 2 = no participa, 3 = archivo vacío, 4 = tipo no
   *     permitido, 5 = archivo demasiado grande
   */
  public int upload(Long userId, Long chatId, MultipartFile file, String contenido, String ip) {
    int acceso = chatServ.checkAccess(userId, chatId);
    if (acceso != 0) {
      return acceso;
    }
    if (file == null || file.isEmpty()) {
      return 3;
    }

    byte[] datos;
    try {
      datos = file.getBytes();
    } catch (IOException e) {
      return 3;
    }

    String mime = detectarTipo(datos);
    if (mime == null) {
      return 4;
    }
    MessageType tipo = mime.startsWith("image/") ? MessageType.IMAGEN : MessageType.AUDIO;
    long maximo = tipo == MessageType.IMAGEN ? MAX_IMAGEN : MAX_AUDIO;
    if (datos.length > maximo) {
      return 5;
    }

    String texto = contenido == null ? "" : contenido.trim();
    if (texto.length() > MAX_CARACTERES) {
      texto = texto.substring(0, MAX_CARACTERES);
    }

    ChatParticipant p = participantRepo.findByChatIdAndUserId(chatId, userId).get();
    Chat chat = p.getChat();
    String llave = crypto.recuperarLlave(chat.getKeyEnc());

    String cifrado = crypto.cifrar(llave, Base64.getEncoder().encodeToString(datos));
    Media media =
        mediaRepo.save(
            new Media(chat, p.getUser(), mime, datos.length, cifrado.getBytes(StandardCharsets.UTF_8)));

    Message m =
        messageRepo.save(
            new Message(chat, p.getUser(), tipo, crypto.cifrar(llave, texto), media.getId()));
    chat.setLastMessageAt(m.getCreatedAt());
    chatRepo.save(chat);
    chatServ.agregarContactoAlRecibir(chatId, userId);

    auditService.log(
        AuditAction.MESSAGE_SENT,
        userId,
        ip,
        "chatId=" + chatId + " messageId=" + m.getId() + " tipo=" + tipo.name() + " mime=" + mime
            + " bytes=" + datos.length);
    return 0;
  }

  /**
   * Busca un archivo si el usuario puede verlo (participa en el chat o es administrador).
   *
   * @return el archivo, o null si no existe o el usuario no tiene permiso
   */
  public Media findAllowed(Long userId, boolean esAdmin, Long mediaId) {
    Optional<Media> found = mediaRepo.findById(mediaId);
    if (!found.isPresent()) {
      return null;
    }
    Media media = found.get();
    if (esAdmin || participantRepo.existsByChatIdAndUserId(media.getChat().getId(), userId)) {
      return media;
    }
    return null;
  }

  /** Descifra el contenido de un archivo con la llave de su chat. */
  public byte[] descifrar(Media media) {
    String llave = crypto.recuperarLlave(media.getChat().getKeyEnc());
    String base64 = crypto.descifrar(llave, new String(media.getDataEnc(), StandardCharsets.UTF_8));
    return Base64.getDecoder().decode(base64);
  }

  /**
   * Detecta el tipo real del archivo por sus primeros bytes.
   *
   * @return el tipo MIME, o null si no es una imagen o un audio permitido
   */
  private String detectarTipo(byte[] d) {
    if (d.length < 12) {
      return null;
    }
    if ((d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
      return "image/jpeg";
    }
    if ((d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
      return "image/png";
    }
    if (d[0] == 'G' && d[1] == 'I' && d[2] == 'F') {
      return "image/gif";
    }
    return null;
  }
  /** Indica si los bytes corresponden a un contenedor RIFF. */
  private boolean esRiff(byte[] d) {
    return d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F';
  }
  /** Detecta el tipo de archivo dentro de un contenedor RIFF. */
  private String detectarTipoRiff(byte[] d) {
    if (d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
      return "image/webp";
    }
    if (d[8] == 'W' && d[9] == 'A' && d[10] == 'V' && d[11] == 'E') {
      return "audio/wav";
    }
    return null;
  }
  /** Detecta si el archivo es audio a partir de sus primeros bytes. */
  private String detectarAudio(byte[] d) {
    if (d[0] == 'O' && d[1] == 'g' && d[2] == 'g' && d[3] == 'S') {
      return "audio/ogg";
    }
    if ((d[0] & 0xFF) == 0x1A && (d[1] & 0xFF) == 0x45 && (d[2] & 0xFF) == 0xDF
        && (d[3] & 0xFF) == 0xA3) {
      return "audio/webm";
    }
    if (d[0] == 'I' && d[1] == 'D' && d[2] == '3') {
      return "audio/mpeg";
    }
    if ((d[0] & 0xFF) == 0xFF && (d[1] & 0xE0) == 0xE0) {
      return "audio/mpeg";
    }
    return null;
  }

  /**
   * Tipo de un archivo con cabecera "ftyp" según su marca. Las fotos HEIC se rechazan porque los
   * navegadores no las muestran.
   *
   * @param marca los cuatro caracteres que siguen a "ftyp"
   * @return el tipo MIME, o null si no está permitido
   */
  private String tipoPorMarca(String marca) {
    if (marca.equals("avif") || marca.equals("avis")) {
      return "image/avif";
    }
    if (marca.startsWith("hei") || marca.startsWith("hev") || marca.equals("mif1")
        || marca.equals("msf1")) {
      return null;
    }
    return "audio/mp4";
  }
}