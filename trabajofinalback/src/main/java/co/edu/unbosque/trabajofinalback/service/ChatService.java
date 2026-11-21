package co.edu.unbosque.trabajofinalback.service;

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
import java.net.URI;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** Maneja las conversaciones y sus mensajes cifrados. */
@Service
public class ChatService {

    private static final String LETRAS_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int MAX_CARACTERES = 2000;

    @Autowired
    private ChatRepository chatRepo;

    @Autowired
    private ChatParticipantRepository participantRepo;

    @Autowired
    private MessageRepository messageRepo;

    @Autowired
    private ContactRepository contactRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ChatCryptoService crypto;

    @Autowired
    private AuditService auditService;

    private final SecureRandom random;

    /** Inicializa el generador de códigos de chat. */
    public ChatService() {
        this.random = new SecureRandom();
    }
    
    /** Abre el chat privado con la IA y lo crea la primera vez. */
    public Long abrirChatIa(Long userId) {
        User usuario = userRepo.buscarParaAbrirChatIa(userId).orElseThrow();

        Optional<Chat> existente = chatRepo.findByAiOwnerId(userId);
        if (existente.isPresent()) {
            return existente.get().getId();
        }

        String llave = crypto.generarLlave();
        Chat chat = new Chat(generarCodigo(), crypto.protegerLlave(llave));
        chat.setAiOwnerId(userId);

        chat = chatRepo.save(chat);
        participantRepo.save(new ChatParticipant(chat, usuario));

        return chat.getId();
    }

    /**
     * Crea un chat entre el usuario y uno de sus contactos.
     * Si ya existe, completa el DTO con el ID y código del chat existente.
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
        if (otro.isEmpty()) {
            return 1;
        }
        if (!contactRepo.existsByOwnerIdAndContactUserId(userId, contactoId)) {
            return 3;
        }

        List<Chat> existentes = chatRepo.findSharedChats(userId, contactoId);
        if (!existentes.isEmpty()) {
            Chat existente = existentes.get(0);
            data.setId(existente.getId());
            data.setCodigo(existente.getCode());
            return 4;
        }

        String llave = crypto.generarLlave();
        Chat chat = chatRepo.save(new Chat(generarCodigo(), crypto.protegerLlave(llave)));

        User usuario = userRepo.findById(userId).orElseThrow();
        participantRepo.save(new ChatParticipant(chat, usuario));
        participantRepo.save(new ChatParticipant(chat, otro.get()));

        data.setId(chat.getId());
        data.setCodigo(chat.getCode());
        return 0;
    }

    /** Retorna 0 si tiene acceso, 1 si el chat no existe y 2 si no participa. */
    public int checkAccess(Long userId, Long chatId) {
        if (!chatRepo.existsById(chatId)) {
            return 1;
        }
        if (!participantRepo.existsByChatIdAndUserId(chatId, userId)) {
            return 2;
        }
        return 0;
    }

    /** Lista los chats del usuario, empezando por el de actividad más reciente. */
    public List<ChatDTO> getAllByUser(Long userId) {
        List<ChatParticipant> participaciones =
                new ArrayList<>(participantRepo.findByUserId(userId));

        participaciones.sort((a, b) ->
                actividad(b.getChat()).compareTo(actividad(a.getChat())));

        List<ChatDTO> resultado = new ArrayList<>();
        for (ChatParticipant participacion : participaciones) {
            resultado.add(toDTO(participacion));
        }
        return resultado;
    }

    /** Obtiene un chat desde la perspectiva de uno de sus participantes. */
    public ChatDTO getById(Long userId, Long chatId) {
        Optional<ChatParticipant> encontrado =
                participantRepo.findByChatIdAndUserId(chatId, userId);

        if (encontrado.isEmpty()) {
            return null;
        }
        return toDTO(encontrado.get());
    }

    /** Actualiza la fecha de la última lectura del participante. */
    public int markAsRead(Long userId, Long chatId) {
        Optional<ChatParticipant> encontrado =
                participantRepo.findByChatIdAndUserId(chatId, userId);

        if (encontrado.isEmpty()) {
            return 1;
        }

        ChatParticipant participante = encontrado.get();
        participante.setLastReadAt(LocalDateTime.now());
        participantRepo.save(participante);
        return 0;
    }

    /** Guarda un texto o GIF y completa el DTO que se devuelve al frontend. */
    public int sendMessage(Long userId, Long chatId, ChatMessageDTO data, String ip) {
        int acceso = checkAccess(userId, chatId);
        if (acceso != 0) {
            return acceso;
        }

        String contenido = data.getContenido();
        if (contenido == null || contenido.isBlank()) {
            return 3;
        }

        contenido = contenido.trim();
        if (contenido.length() > MAX_CARACTERES) {
            return 4;
        }

        MessageType tipo = MessageType.TEXTO;
        if ("GIF".equals(data.getTipo())) {
            if (!esUrlDeGiphy(contenido)) {
                return 5;
            }
            tipo = MessageType.GIF;
        } else if (data.getTipo() != null && !"TEXTO".equals(data.getTipo())) {
            return 6;
        }

        ChatParticipant participante =
                participantRepo.findByChatIdAndUserId(chatId, userId).orElseThrow();
        Chat chat = participante.getChat();
        String llave = crypto.recuperarLlave(chat.getKeyEnc());

        Message mensaje = new Message(
                chat, participante.getUser(), tipo, crypto.cifrar(llave, contenido), null);
        mensaje = messageRepo.save(mensaje);

        chat.setLastMessageAt(mensaje.getCreatedAt());
        chatRepo.save(chat);

        auditService.log(
                AuditAction.MESSAGE_SENT, userId, ip,
                "chatId=" + chatId + " messageId=" + mensaje.getId()
                        + " tipo=" + tipo.name() + " caracteres=" + contenido.length());

        data.setId(mensaje.getId());
        data.setChatId(chatId);
        data.setRemitenteId(userId);
        data.setRemitenteNombre(participante.getUser().getUsername());
        data.setContenido(contenido);
        data.setTipo(tipo.name());
        data.setMediaUrl(null);
        data.setFechaEnvio(mensaje.getCreatedAt().toString());
        return 0;
    }

    /** Valida los enlaces HTTPS de Giphy que se envían como mensajes. */
    private boolean esUrlDeGiphy(String contenido) {
        try {
            URI uri = URI.create(contenido);
            String host = uri.getHost();

            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || host == null || uri.getUserInfo() != null) {
                return false;
            }

            host = host.toLowerCase(Locale.ROOT);
            return host.equals("giphy.com") || host.endsWith(".giphy.com");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Obtiene una página del historial, ordenada del más reciente al más antiguo. */
    public List<ChatMessageDTO> getMessages(Long chatId, int page, int size) {
        if (page < 0) {
            page = 0;
        }
        if (size < 1 || size > 50) {
            size = 20;
        }

        Chat chat = chatRepo.findById(chatId).orElseThrow();
        String llave = crypto.recuperarLlave(chat.getKeyEnc());
        Page<Message> pagina = messageRepo.findByChatIdOrderByIdDesc(
                chatId, PageRequest.of(page, size));

        List<ChatMessageDTO> resultado = new ArrayList<>();
        for (Message mensaje : pagina.getContent()) {
            resultado.add(toMessageDTO(mensaje, llave));
        }
        return resultado;
    }

    /** Devuelve los mensajes posteriores a afterId en orden de llegada. */
    public List<ChatMessageDTO> getNewMessages(Long chatId, Long afterId) {
        if (afterId == null || afterId < 0) {
            afterId = 0L;
        }

        List<Message> nuevos =
                messageRepo.findByChatIdAndIdGreaterThanOrderByIdAsc(chatId, afterId);
        return toMessageDTOList(nuevos);
    }

    /** Lista los mensajes enviados por un usuario en todos sus chats. */
    public List<ChatMessageDTO> getSent(Long userId, int page, int size) {
        if (page < 0) {
            page = 0;
        }
        if (size < 1 || size > 50) {
            size = 20;
        }

        List<Message> enviados =
                messageRepo.findBySenderIdOrderByIdDesc(userId, PageRequest.of(page, size));
        return toMessageDTOList(enviados);
    }

    /** Lista los mensajes recibidos por el usuario después de afterId. */
    public List<ChatMessageDTO> getReceived(Long userId, Long afterId, int page, int size) {
        if (afterId == null || afterId < 0) {
            afterId = 0L;
        }
        if (page < 0) {
            page = 0;
        }
        if (size < 1 || size > 50) {
            size = 20;
        }

        List<Message> recibidos =
                messageRepo.findReceived(userId, afterId, PageRequest.of(page, size));
        return toMessageDTOList(recibidos);
    }

    /** Descifra mensajes de varios chats reutilizando la llave durante la consulta. */
    private List<ChatMessageDTO> toMessageDTOList(List<Message> mensajes) {
        Map<Long, String> llaves = new HashMap<>();
        List<ChatMessageDTO> resultado = new ArrayList<>();

        for (Message mensaje : mensajes) {
            Chat chat = mensaje.getChat();
            String llave = llaves.get(chat.getId());

            if (llave == null) {
                llave = crypto.recuperarLlave(chat.getKeyEnc());
                llaves.put(chat.getId(), llave);
            }

            resultado.add(toMessageDTO(mensaje, llave));
        }
        return resultado;
    }

    /** Construye el resumen que ve un participante en su lista de chats. */
    private ChatDTO toDTO(ChatParticipant participacion) {
        Chat chat = participacion.getChat();
        Long usuarioId = participacion.getUser().getId();

        ChatDTO dto = new ChatDTO();
        dto.setId(chat.getId());
        dto.setCodigo(chat.getCode());
        dto.setFechaCreacion(chat.getCreatedAt().toString());

        List<ContactDTO> participantes = new ArrayList<>();
        for (ChatParticipant otro : participantRepo.findByChatIdOrderByIdAsc(chat.getId())) {
            User usuario = otro.getUser();
            participantes.add(new ContactDTO(
                    usuario.getId(), usuario.getUsername(), usuario.getUsername(), null));
        }
        dto.setParticipantes(participantes);

        Optional<Message> ultimo = messageRepo.findFirstByChatIdOrderByIdDesc(chat.getId());
        if (ultimo.isPresent()) {
            String llave = crypto.recuperarLlave(chat.getKeyEnc());
            dto.setUltimoMensaje(toMessageDTO(ultimo.get(), llave));
        }

        if (participacion.getLastReadAt() == null) {
            dto.setNoLeidos(messageRepo.countByChatIdAndSenderIdNot(chat.getId(), usuarioId));
        } else {
            dto.setNoLeidos(messageRepo.countByChatIdAndSenderIdNotAndCreatedAtAfter(
                    chat.getId(), usuarioId, participacion.getLastReadAt()));
        }

        return dto;
    }

    /** Convierte un mensaje guardado en el DTO que consume Angular. */
    private ChatMessageDTO toMessageDTO(Message mensaje, String llave) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(mensaje.getId());
        dto.setChatId(mensaje.getChat().getId());

        if (mensaje.getSender() != null) {
            dto.setRemitenteId(mensaje.getSender().getId());
            dto.setRemitenteNombre(mensaje.getSender().getUsername());
        } else {
            dto.setRemitenteNombre("IA");
        }

        dto.setContenido(crypto.descifrar(llave, mensaje.getContentEnc()));
        dto.setTipo(mensaje.getType().name());

        if (mensaje.getMediaId() != null) {
            dto.setMediaUrl("/media/getbyid/" + mensaje.getMediaId());
        }

        dto.setFechaEnvio(mensaje.getCreatedAt().toString());
        return dto;
    }

    /** Genera un código de conversación que todavía no esté registrado. */
    private String generarCodigo() {
        String codigo;
        do {
            StringBuilder resultado = new StringBuilder("CH-");
            for (int i = 0; i < 6; i++) {
                resultado.append(LETRAS_CODIGO.charAt(random.nextInt(LETRAS_CODIGO.length())));
            }
            codigo = resultado.toString();
        } while (chatRepo.existsByCode(codigo));

        return codigo;
    }

    /** Usa la creación del chat como fecha de actividad hasta el primer mensaje. */
    private LocalDateTime actividad(Chat chat) {
        if (chat.getLastMessageAt() != null) {
            return chat.getLastMessageAt();
        }
        return chat.getCreatedAt();
    }
}