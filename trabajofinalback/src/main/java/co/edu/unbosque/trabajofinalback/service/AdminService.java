package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.dto.AuditAdminDTO;
import co.edu.unbosque.trabajofinalback.dto.ChatAdminDTO;
import co.edu.unbosque.trabajofinalback.dto.MensajeAdminDTO;
import co.edu.unbosque.trabajofinalback.exception.InvalidTokenException;
import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.model.AuditLog;
import co.edu.unbosque.trabajofinalback.model.Chat;
import co.edu.unbosque.trabajofinalback.model.ChatParticipant;
import co.edu.unbosque.trabajofinalback.model.Message;
import co.edu.unbosque.trabajofinalback.model.User;
import co.edu.unbosque.trabajofinalback.repository.AuditLogRepository;
import co.edu.unbosque.trabajofinalback.repository.ChatParticipantRepository;
import co.edu.unbosque.trabajofinalback.repository.ChatRepository;
import co.edu.unbosque.trabajofinalback.repository.MessageRepository;
import co.edu.unbosque.trabajofinalback.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Consultas del panel de administración: auditoría y lectura de chats. */
@Service
public class AdminService {

	/** Máximo de eventos de auditoría que se devuelven en una consulta. */
	private static final int LIMITE_MAXIMO_AUDITORIA = 500;

	/** Máximo de mensajes que se devuelven al leer un chat. */
	private static final int LIMITE_MENSAJES = 200;

	/** Texto que se muestra cuando un mensaje no se puede descifrar. */
	private static final String MENSAJE_ILEGIBLE = "[No se pudo descifrar el mensaje]";

	private final AuditLogRepository auditRepo;
	private final UserRepository userRepo;
	private final ChatRepository chatRepo;
	private final ChatParticipantRepository participantRepo;
	private final MessageRepository messageRepo;
	private final ChatCryptoService crypto;
	private final AuditService auditService;

	/**
	 * Crea el servicio con los repositorios y servicios que necesita.
	 *
	 * @param auditRepo       repositorio de eventos de auditoría
	 * @param userRepo        repositorio de usuarios
	 * @param chatRepo        repositorio de chats
	 * @param participantRepo repositorio de participantes de chat
	 * @param messageRepo     repositorio de mensajes
	 * @param crypto          servicio de cifrado de chats
	 * @param auditService    servicio para registrar auditoría
	 */
	public AdminService(AuditLogRepository auditRepo, UserRepository userRepo, ChatRepository chatRepo,
			ChatParticipantRepository participantRepo, MessageRepository messageRepo, ChatCryptoService crypto,
			AuditService auditService) {
		this.auditRepo = auditRepo;
		this.userRepo = userRepo;
		this.chatRepo = chatRepo;
		this.participantRepo = participantRepo;
		this.messageRepo = messageRepo;
		this.crypto = crypto;
		this.auditService = auditService;
	}

	/**
	 * Devuelve los últimos eventos de auditoría con el nombre de usuario.
	 *
	 * @param limit cantidad de eventos a devolver (se ajusta entre 1 y 500)
	 * @return eventos de auditoría, del más reciente al más antiguo
	 */
	@Transactional(readOnly = true)
	public List<AuditAdminDTO> audit(int limit) {
		int tamano = Math.max(1, Math.min(limit, LIMITE_MAXIMO_AUDITORIA));

		PageRequest pagina = PageRequest.of(0, tamano, Sort.by(Sort.Direction.DESC, "id"));
		List<AuditLog> eventos = auditRepo.findAll(pagina).getContent();

		Set<Long> ids = new HashSet<>();
		for (AuditLog evento : eventos) {
			if (evento.getUserId() != null) {
				ids.add(evento.getUserId());
			}
		}

		Map<Long, String> nombres = new HashMap<>();
		for (User usuario : userRepo.findAllById(ids)) {
			nombres.put(usuario.getId(), usuario.getUsername());
		}

		List<AuditAdminDTO> resultado = new ArrayList<>(eventos.size());
		for (AuditLog evento : eventos) {
			String accion = evento.getAction() == null ? null : evento.getAction().name();
			AuditAdminDTO fila = new AuditAdminDTO(evento.getId(), nombres.get(evento.getUserId()), accion,
					evento.getIp(), evento.getLocation(), evento.getDetail(), evento.getCreatedAt());
			resultado.add(fila);
		}

		return resultado;
	}

	/**
	 * Devuelve todos los chats con participantes y cantidad de mensajes.
	 *
	 * @return chats ordenados por fecha del último mensaje, del más reciente al más antiguo
	 */
	@Transactional(readOnly = true)
	public List<ChatAdminDTO> chats() {
		List<ChatAdminDTO> resultado = new ArrayList<>();

		for (Chat chat : chatRepo.findAllByOrderByLastMessageAtDesc()) {
			Long chatId = chat.getId();
			List<String> nombres = new ArrayList<>();

			for (ChatParticipant participante : participantRepo.findByChatIdOrderByIdAsc(chatId)) {
				if (participante.getUser() != null) {
					nombres.add(participante.getUser().getUsername());
				}
			}

			ChatAdminDTO fila = new ChatAdminDTO(chatId, chat.getCode(), nombres, chat.getLastMessageAt(),
					messageRepo.countByChatId(chatId));
			resultado.add(fila);
		}

		return resultado;
	}

	/**
	 * Devuelve los mensajes de un chat descifrados si la clave es correcta.
	 *
	 * @param chatId  identificador del chat
	 * @param key     clave del chat escrita por el administrador
	 * @param adminId identificador del administrador que consulta
	 * @param ip      dirección IP desde la que se consulta
	 * @return los últimos mensajes del chat, del más reciente al más antiguo
	 * @throws InvalidTokenException si la clave es incorrecta
	 */
	@Transactional
	public List<MensajeAdminDTO> messages(Long chatId, String key, Long adminId, String ip) {
		Chat chat = buscarChat(chatId);
		String claveReal = crypto.recuperarLlave(chat.getKeyEnc());

		if (!clavesIguales(claveReal, key)) {
			auditService.log(AuditAction.ADMIN_READ_CHAT, adminId, ip,
					"Intento fallido (clave incorrecta), chat=" + chat.getCode());
			throw new InvalidTokenException("Clave incorrecta", HttpStatus.BAD_REQUEST);
		}

		auditService.log(AuditAction.ADMIN_READ_CHAT, adminId, ip, "chat=" + chat.getCode());

		PageRequest pagina = PageRequest.of(0, LIMITE_MENSAJES);
		List<Message> mensajes = messageRepo.findByChatIdOrderByIdDesc(chatId, pagina).getContent();
		List<MensajeAdminDTO> resultado = new ArrayList<>(mensajes.size());

		for (Message mensaje : mensajes) {
			String remitente = mensaje.getSender() == null ? null : mensaje.getSender().getUsername();
			String tipo = mensaje.getType() == null ? null : mensaje.getType().name();

			MensajeAdminDTO fila = new MensajeAdminDTO(mensaje.getId(), remitente, tipo,
					descifrarSeguro(claveReal, mensaje.getContentEnc()), mensaje.getMediaId(),
					mensaje.getCreatedAt());
			resultado.add(fila);
		}

		return resultado;
	}

	/**
	 * Devuelve la clave de un chat ya descifrada.
	 *
	 * @param chatId  identificador del chat
	 * @param adminId identificador del administrador que consulta
	 * @param ip      dirección IP desde la que se consulta
	 * @return la clave del chat en texto plano
	 */
	@Transactional
	public String chatKey(Long chatId, Long adminId, String ip) {
		Chat chat = buscarChat(chatId);
		auditService.log(AuditAction.ADMIN_READ_CHAT, adminId, ip, "clave del chat=" + chat.getCode());
		return crypto.recuperarLlave(chat.getKeyEnc());
	}

	/** Busca un chat por id o responde 404 si no existe. */
	private Chat buscarChat(Long chatId) {
		return chatRepo.findById(chatId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat no encontrado"));
	}

	/** Compara dos claves en tiempo constante; devuelve false si alguna es null. */
	private static boolean clavesIguales(String esperada, String recibida) {
		if (esperada == null || recibida == null) {
			return false;
		}
		return MessageDigest.isEqual(esperada.getBytes(StandardCharsets.UTF_8),
				recibida.getBytes(StandardCharsets.UTF_8));
	}

	/** Descifra un mensaje; si falla, devuelve un texto de aviso en vez de romper toda la lista. */
	private String descifrarSeguro(String clave, String contenidoCifrado) {
		if (contenidoCifrado == null) {
			return null;
		}
		try {
			return crypto.descifrar(clave, contenidoCifrado);
		} catch (RuntimeException e) {
			return MENSAJE_ILEGIBLE;
		}
	}
}