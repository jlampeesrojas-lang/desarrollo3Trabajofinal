package co.edu.unbosque.trabajofinalback.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import co.edu.unbosque.trabajofinalback.dto.ChatDTO;
import co.edu.unbosque.trabajofinalback.dto.ChatMessageDTO;
import co.edu.unbosque.trabajofinalback.dto.ContactDTO;
import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.model.Chat;
import co.edu.unbosque.trabajofinalback.model.ChatParticipant;
import co.edu.unbosque.trabajofinalback.model.Message;
import co.edu.unbosque.trabajofinalback.model.MessageType;
import co.edu.unbosque.trabajofinalback.model.User;
import co.edu.unbosque.trabajofinalback.repository.ChatParticipantRepository;
import co.edu.unbosque.trabajofinalback.repository.ChatRepository;
import co.edu.unbosque.trabajofinalback.repository.ContactRepository;
import co.edu.unbosque.trabajofinalback.repository.MessageRepository;
import co.edu.unbosque.trabajofinalback.repository.UserRepository;

/** Lógica de los chats 1 a 1 y de sus mensajes cifrados. */
@Service
public class ChatService {

  private static final String LETRAS_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

  private static final int MAX_CARACTERES = 2000;

  @Autowired private ChatRepository chatRepo;

  @Autowired private ChatParticipantRepository participantRepo;

  @Autowired private MessageRepository messageRepo;

  @Autowired private ContactRepository contactRepo;

  @Autowired private UserRepository userRepo;

  @Autowired private ChatCryptoService crypto;

  @Autowired private AuditService auditService;

  private SecureRandom random = new SecureRandom();

  public ChatService() {}

  // =================================================================== CHATS

  /**
   * Crea un chat 1 a 1 con un contacto. Llena el id y el código en el mismo DTO.
   *
   * @return 0 = creado, 1 = el usuario no existe, 2 = es uno mismo, 3 = no está en tus contactos,
   *     4 = ya existía un chat con ese contacto
   */
  public int create(Long userId, ChatDTO data) {
    Long contactoId = data.getContactoId();
    if (contactoId == null) {
      return 1;
    }
    if (contactoId.equals(userId)) {
      return 2;
    }
    Optional<User> otro = userRepo.findById(contactoId);
    if (!otro.isPresent()) {
      return 1;
    }
    if (!contactRepo.existsByOwnerIdAndContactUserId(userId, contactoId)) {
      return 3;
    }

    List<Chat> existentes = chatRepo.findSharedChats(userId, contactoId);
    if (!existentes.isEmpty()) {
      data.setId(existentes.get(0).getId());
      data.setCodigo(existentes.get(0).getCode());
      return 4;
    }

    // Llave AES propia del chat; en la base de datos se guarda cifrada
    String llave = crypto.generarLlave();
    Chat chat = chatRepo.save(new Chat(generarCodigo(), crypto.protegerLlave(llave)));
    participantRepo.save(new ChatParticipant(chat, userRepo.findById(userId).get()));
    participantRepo.save(new ChatParticipant(chat, otro.get()));

    data.setId(chat.getId());
    data.setCodigo(chat.getCode());
    return 0;
  }

  /**
   * Revisa si el usuario puede entrar al chat.
   *
   * @return 0 = puede, 1 = el chat no existe, 2 = no participa en el chat
   */
  public int checkAccess(Long userId, Long chatId) {
    if (!chatRepo.existsById(chatId)) {
      return 1;
    }
    if (!participantRepo.existsByChatIdAndUserId(chatId, userId)) {
      return 2;
    }
    return 0;
  }

  /** Chats del usuario, el de actividad más reciente primero. */
  public List<ChatDTO> getAllByUser(Long userId) {
    List<ChatParticipant> mias = new ArrayList<>(participantRepo.findByUserId(userId));
    mias.sort((a, b) -> actividad(b.getChat()).compareTo(actividad(a.getChat())));

    List<ChatDTO> dtoList = new ArrayList<>();
    mias.forEach(
        (p) -> {
          dtoList.add(toDTO(p));
        });
    return dtoList;
  }

  /** Un chat del usuario, o null si no existe o no participa. */
  public ChatDTO getById(Long userId, Long chatId) {
    Optional<ChatParticipant> found = participantRepo.findByChatIdAndUserId(chatId, userId);
    if (found.isPresent()) {
      return toDTO(found.get());
    } else {
      return null;
    }
  }

  /** Marca el chat como leído: los no leídos vuelven a 0. */
  public int markAsRead(Long userId, Long chatId) {
    Optional<ChatParticipant> found = participantRepo.findByChatIdAndUserId(chatId, userId);
    if (found.isPresent()) {
      ChatParticipant p = found.get();
      p.setLastReadAt(LocalDateTime.now());
      participantRepo.save(p);
      return 0;
    } else {
      return 1;
    }
  }

  // ================================================================ MENSAJES

  /**
   * Cifra y guarda un mensaje de texto. Llena los datos del mensaje guardado en el mismo DTO.
   *
   * @return 0 = enviado, 1 = el chat no existe, 2 = no participa, 3 = mensaje vacío, 4 = más de
   *     2000 caracteres
   */
  public int sendMessage(Long userId, Long chatId, ChatMessageDTO data, String ip) {
    int acceso = checkAccess(userId, chatId);
    if (acceso != 0) {
      return acceso;
    }
    String texto = data.getContenido();
    if (texto == null || texto.isBlank()) {
      return 3;
    }
    if (texto.length() > MAX_CARACTERES) {
      return 4;
    }

    ChatParticipant p = participantRepo.findByChatIdAndUserId(chatId, userId).get();
    Chat chat = p.getChat();

    // Se cifra con la llave del chat ANTES de guardar
    String llave = crypto.recuperarLlave(chat.getKeyEnc());
    Message m =
        messageRepo.save(
            new Message(chat, p.getUser(), MessageType.TEXTO, crypto.cifrar(llave, texto), null));
    chat.setLastMessageAt(m.getCreatedAt());
    chatRepo.save(chat);

    // Metadatos del mensaje en la auditoría: NUNCA el texto
    auditService.log(
        AuditAction.MESSAGE_SENT,
        userId,
        ip,
        "chatId=" + chatId + " messageId=" + m.getId() + " tipo=TEXTO caracteres=" + texto.length());

    data.setId(m.getId());
    data.setChatId(chatId);
    data.setRemitenteId(userId);
    data.setRemitenteNombre(p.getUser().getUsername());
    data.setTipo(MessageType.TEXTO.name());
    data.setFechaEnvio(m.getCreatedAt().toString());
    return 0;
  }

  /**
   * Historial paginado y descifrado. Página 0 = los mensajes más recientes.
   *
   * @param size cuántos mensajes por página (de 1 a 50; por defecto 20)
   */
  public List<ChatMessageDTO> getMessages(Long chatId, int page, int size) {
    if (page < 0) {
      page = 0;
    }
    if (size < 1 || size > 50) {
      size = 20;
    }
    Chat chat = chatRepo.findById(chatId).get();
    String llave = crypto.recuperarLlave(chat.getKeyEnc());

    Page<Message> pagina = messageRepo.findByChatIdOrderByIdDesc(chatId, PageRequest.of(page, size));

    List<ChatMessageDTO> dtoList = new ArrayList<>();
    pagina
        .getContent()
        .forEach(
            (m) -> {
              dtoList.add(toMessageDTO(m, llave));
            });
    return dtoList;
  }

  // =============================================================== UTILIDADES

  /** Pasa un chat a DTO, visto desde el usuario de esa participación. */
  private ChatDTO toDTO(ChatParticipant mia) {
    Chat chat = mia.getChat();
    Long yoId = mia.getUser().getId();

    ChatDTO dto = new ChatDTO();
    dto.setId(chat.getId());
    dto.setCodigo(chat.getCode());
    dto.setFechaCreacion(chat.getCreatedAt().toString());

    List<ContactDTO> participantes = new ArrayList<>();
    participantRepo
        .findByChatIdOrderByIdAsc(chat.getId())
        .forEach(
            (cp) -> {
              User u = cp.getUser();
              participantes.add(new ContactDTO(u.getId(), u.getUsername(), u.getUsername(), null));
            });
    dto.setParticipantes(participantes);

    String llave = crypto.recuperarLlave(chat.getKeyEnc());
    Optional<Message> ultimo = messageRepo.findFirstByChatIdOrderByIdDesc(chat.getId());
    if (ultimo.isPresent()) {
      dto.setUltimoMensaje(toMessageDTO(ultimo.get(), llave));
    }

    if (mia.getLastReadAt() == null) {
      dto.setNoLeidos(messageRepo.countByChatIdAndSenderIdNot(chat.getId(), yoId));
    } else {
      dto.setNoLeidos(
          messageRepo.countByChatIdAndSenderIdNotAndCreatedAtAfter(
              chat.getId(), yoId, mia.getLastReadAt()));
    }
    return dto;
  }

  /** Pasa un mensaje a DTO descifrando su texto. */
  private ChatMessageDTO toMessageDTO(Message m, String llave) {
    ChatMessageDTO dto = new ChatMessageDTO();
    dto.setId(m.getId());
    dto.setChatId(m.getChat().getId());
    if (m.getSender() != null) {
      dto.setRemitenteId(m.getSender().getId());
      dto.setRemitenteNombre(m.getSender().getUsername());
    } else {
      dto.setRemitenteNombre("IA");
    }
    dto.setContenido(crypto.descifrar(llave, m.getContentEnc()));
    dto.setTipo(m.getType().name());
    if (m.getMediaId() != null) {
      dto.setMediaUrl("/media/getbyid/" + m.getMediaId());
    }
    dto.setFechaEnvio(m.getCreatedAt().toString());
    return dto;
  }

  /** Genera un código único tipo CH-7KQ2MZ (sin letras confusas como O, 0, I, 1). */
  private String generarCodigo() {
    String codigo;
    do {
      StringBuilder sb = new StringBuilder("CH-");
      for (int i = 0; i < 6; i++) {
        sb.append(LETRAS_CODIGO.charAt(random.nextInt(LETRAS_CODIGO.length())));
      }
      codigo = sb.toString();
    } while (chatRepo.existsByCode(codigo));
    return codigo;
  }

  /** Fecha de la última actividad del chat (último mensaje, o creación si no hay mensajes). */
  private LocalDateTime actividad(Chat c) {
    if (c.getLastMessageAt() != null) {
      return c.getLastMessageAt();
    }
    return c.getCreatedAt();
  }
}