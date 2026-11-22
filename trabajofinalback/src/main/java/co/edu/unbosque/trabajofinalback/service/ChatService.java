package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.dto.ChatDTO;
import co.edu.unbosque.trabajofinalback.dto.ChatMessageDTO;
import co.edu.unbosque.trabajofinalback.dto.ContactDTO;
import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.model.Chat;
import co.edu.unbosque.trabajofinalback.model.ChatParticipant;
import co.edu.unbosque.trabajofinalback.model.Contact;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Maneja las conversaciones, los contactos del receptor y los mensajes cifrados. */
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
    private ChatUnlockService permisos;
    @Autowired
    private AuditService auditService;
    private final SecureRandom random;

    /** Inicializa el generador de códigos de chat. */
    public ChatService() {
        random = new SecureRandom();
    }

    /** Abre el chat privado con la IA y lo crea la primera vez. */
    @Transactional
    public Long abrirChatIa(Long userId) {
        User usuario = userRepo.findLockedById(userId).orElseThrow();
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

    /** Crea un chat con un contacto o devuelve los datos del chat existente. */
    @Transactional
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

        for (ChatParticipant participacion : participantRepo.findByUserId(userId)) {
            Chat existente = participacion.getChat();

            if (participantRepo.existsByChatIdAndUserId(existente.getId(), contactoId)) {
                data.setId(existente.getId());
                data.setCodigo(existente.getCode());
                return 4;
            }
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

        participaciones.sort(new Comparator<ChatParticipant>() {
            public int compare(ChatParticipant primero, ChatParticipant segundo) {
                LocalDateTime actividadPrimero = actividad(primero.getChat());
                LocalDateTime actividadSegundo = actividad(segundo.getChat());
                return actividadSegundo.compareTo(actividadPrimero);
            }
        });

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

    /** Guarda un texto o GIF y completa el DTO que devuelve el controlador. */
    @Transactional
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

        agregarContactoAlRecibir(chatId, userId);
        chat.setLastMessageAt(mensaje.getCreatedAt());
        chatRepo.save(chat);

        auditService.log(AuditAction.MESSAGE_SENT, userId, ip,
                "chatId=" + chatId + " messageId=" + mensaje.getId()
                        + " tipo=" + tipo.name() + " caracteres=" + contenido.length());

        completarDTO(data, mensaje, llave, permisos.abierto(chatId));
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

    /** Obtiene una página del historial visible para la pestaña actual. */
    public List<ChatMessageDTO> getMessages(Long chatId, int page, int size) {
        return leerPagina(chatId, page, size, permisos.abierto(chatId));
    }

    /** Obtiene el contexto legible que usa AiService después de validar el acceso. */
    List<ChatMessageDTO> historialParaIa(Long chatId, int cantidad) {
        return leerPagina(chatId, 0, cantidad, true);
    }

    /** Lee los mensajes del más reciente al más antiguo. */
    private List<ChatMessageDTO> leerPagina(
            Long chatId, int page, int size, boolean legible) {
        Page<Message> pagina = messageRepo.findByChatIdOrderByIdDesc(
                chatId, paginacion(page, size));

        String llave = null;
        if (legible && pagina.hasContent()) {
            Chat chat = pagina.getContent().get(0).getChat();
            llave = crypto.recuperarLlave(chat.getKeyEnc());
        }

        List<ChatMessageDTO> resultado = new ArrayList<>();
        for (Message mensaje : pagina.getContent()) {
            resultado.add(toMessageDTO(mensaje, llave, legible));
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
        List<Message> enviados =
                messageRepo.findBySenderIdOrderByIdDesc(userId, paginacion(page, size));
        return toMessageDTOList(enviados);
    }

    /** Lista los mensajes recibidos después del id indicado, incluidos los de IA. */
    public List<ChatMessageDTO> getReceived(Long userId, Long afterId, int page, int size) {
        if (afterId == null || afterId < 0) {
            afterId = 0L;
        }

        PageRequest solicitud = paginacion(page, size);
        List<Long> chats = new ArrayList<>();
        for (ChatParticipant participante : participantRepo.findByUserId(userId)) {
            chats.add(participante.getChat().getId());
        }

        List<Message> recibidos = new ArrayList<>();
        long mensajesParaSaltar = solicitud.getOffset();
        int limite = solicitud.getPageSize();
        long antes = Long.MAX_VALUE;

        while (!chats.isEmpty() && recibidos.size() < limite) {
            List<Message> bloque =
                    messageRepo.findByChatIdInAndIdGreaterThanAndIdLessThanOrderByIdDesc(
                            chats, afterId, antes, PageRequest.of(0, 100));

            for (Message mensaje : bloque) {
                antes = mensaje.getId();

                if (mensaje.getSender() != null
                        && userId.equals(mensaje.getSender().getId())) {
                    continue;
                }
                if (mensajesParaSaltar > 0) {
                    mensajesParaSaltar--;
                    continue;
                }

                recibidos.add(mensaje);
                if (recibidos.size() == limite) {
                    break;
                }
            }

            if (bloque.size() < 100) {
                break;
            }
        }

        return toMessageDTOList(recibidos);
    }

    /** Aplica los límites de página que usan las consultas de mensajes. */
    private PageRequest paginacion(int page, int size) {
        if (page < 0) {
            page = 0;
        }
        if (size < 1 || size > 50) {
            size = 20;
        }
        return PageRequest.of(page, size);
    }

    /** Comprueba el permiso una sola vez por chat dentro de esta lista. */
    private List<ChatMessageDTO> toMessageDTOList(List<Message> mensajes) {
        Map<Long, String> llaves = new HashMap<>();
        List<ChatMessageDTO> resultado = new ArrayList<>();

        for (Message mensaje : mensajes) {
            Chat chat = mensaje.getChat();
            Long chatId = chat.getId();

            if (!llaves.containsKey(chatId)) {
                String llave = null;
                if (permisos.abierto(chatId)) {
                    llave = crypto.recuperarLlave(chat.getKeyEnc());
                }
                llaves.put(chatId, llave);
            }

            String llave = llaves.get(chatId);
            resultado.add(toMessageDTO(mensaje, llave, llave != null));
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
            boolean legible = permisos.abierto(chat.getId());
            String llave = null;
            if (legible) {
                llave = crypto.recuperarLlave(chat.getKeyEnc());
            }
            dto.setUltimoMensaje(toMessageDTO(ultimo.get(), llave, legible));
        }

        if (participacion.getLastReadAt() == null) {
            dto.setNoLeidos(messageRepo.countByChatIdAndSenderIdNot(chat.getId(), usuarioId));
        } else {
            dto.setNoLeidos(messageRepo.countByChatIdAndSenderIdNotAndCreatedAtAfter(
                    chat.getId(), usuarioId, participacion.getLastReadAt()));
        }
        return dto;
    }

    /** Crea el DTO para una burbuja del historial. */
    private ChatMessageDTO toMessageDTO(Message mensaje, String llave, boolean legible) {
        ChatMessageDTO dto = new ChatMessageDTO();
        completarDTO(dto, mensaje, llave, legible);
        return dto;
    }

    /** Aplica la misma presentación al historial y a la respuesta de envío. */
    private void completarDTO(
            ChatMessageDTO dto, Message mensaje, String llave, boolean legible) {
        dto.setId(mensaje.getId());
        dto.setChatId(mensaje.getChat().getId());
        dto.setFechaEnvio(mensaje.getCreatedAt().toString());

        if (mensaje.getSender() == null) {
            dto.setRemitenteId(null);
            dto.setRemitenteNombre("IA");
        } else {
            dto.setRemitenteId(mensaje.getSender().getId());
            dto.setRemitenteNombre(mensaje.getSender().getUsername());
        }

        dto.setMediaUrl(null);
        dto.setLeyenda(null);

        if (legible) {
            dto.setContenido(crypto.descifrar(llave, mensaje.getContentEnc()));
            dto.setTipo(mensaje.getType().name());

            if (mensaje.getCaptionEnc() != null) {
                dto.setLeyenda(crypto.descifrar(llave, mensaje.getCaptionEnc()));
            }

            if (mensaje.getMediaId() != null) {
                dto.setMediaUrl("/media/getbyid/" + mensaje.getMediaId());
            }
        } else {
            dto.setContenido(mensaje.getContentEnc());
            dto.setTipo("CIFRADO");
        }
    }

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

    /** Usa la fecha de creación hasta que el chat tenga su primer mensaje. */
    private LocalDateTime actividad(Chat chat) {
        if (chat.getLastMessageAt() != null) {
            return chat.getLastMessageAt();
        }
        return chat.getCreatedAt();
    }

    /** Agrega al remitente a los contactos del receptor y conserva el alias existente. */
    @Transactional
    public void agregarContactoAlRecibir(Long chatId, Long remitenteId) {
        List<ChatParticipant> participantes =
                participantRepo.findByChatIdOrderByIdAsc(chatId);

        for (ChatParticipant participante : participantes) {
            Long receptorId = participante.getUser().getId();
            if (receptorId.equals(remitenteId)) {
                continue;
            }

            User receptor = userRepo.findLockedById(receptorId).orElseThrow();
            Optional<Contact> existente =
                    contactRepo.findLockedByOwnerIdAndContactUserId(receptorId, remitenteId);

            if (existente.isEmpty()) {
                User remitente = userRepo.getReferenceById(remitenteId);
                contactRepo.save(new Contact(receptor, remitente, null));
            }
        }
    }
}