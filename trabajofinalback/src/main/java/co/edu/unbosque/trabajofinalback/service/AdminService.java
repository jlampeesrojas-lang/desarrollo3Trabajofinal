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
import co.edu.unbosque.trabajofinalback.repository.RefreshTokenRepository;
import co.edu.unbosque.trabajofinalback.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Operaciones del panel de administración: auditoría, usuarios y lectura de
 * chats.
 */
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
	private final RefreshTokenRepository refreshTokenRepo;
	private final ChatCryptoService crypto;
	private final AuditService auditService;

	/**
	 * Crea el servicio con los repositorios y servicios que necesita.
	 *
	 * @param auditRepo        repositorio de eventos de auditoría
	 * @param userRepo         repositorio de usuarios
	 * @param chatRepo         repositorio de chats
	 * @param participantRepo  repositorio de participantes de chat
	 * @param messageRepo      repositorio de mensajes
	 * @param refreshTokenRepo repositorio de sesiones (refresh tokens)
	 * @param crypto           servicio de cifrado de chats
	 * @param auditService     servicio para registrar auditoría
	 */
	public AdminService(AuditLogRepository auditRepo, UserRepository userRepo, ChatRepository chatRepo,
			ChatParticipantRepository participantRepo, MessageRepository messageRepo,
			RefreshTokenRepository refreshTokenRepo, ChatCryptoService crypto, AuditService auditService) {
		this.auditRepo = auditRepo;
		this.userRepo = userRepo;
		this.chatRepo = chatRepo;
		this.participantRepo = participantRepo;
		this.messageRepo = messageRepo;
		this.refreshTokenRepo = refreshTokenRepo;
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
		return audit(limit, null, null, null, null, null);
	}

	/**
	 * Devuelve eventos de auditoría con filtros opcionales. Un filtro en null no se
	 * aplica.
	 *
	 * @param limit    cantidad de eventos a devolver (se ajusta entre 1 y 500)
	 * @param userId   id del usuario
	 * @param username nombre del usuario (si se envía, reemplaza a userId)
	 * @param action   tipo de evento
	 * @param desde    fecha y hora inicial (incluida)
	 * @param hasta    fecha y hora final (excluida)
	 * @return eventos de auditoría, del más reciente al más antiguo
	 */
	@Transactional(readOnly = true)
	public List<AuditAdminDTO> audit(int limit, Long userId, String username, AuditAction action, LocalDateTime desde,
			LocalDateTime hasta) {
		int tamano = Math.max(1, Math.min(limit, LIMITE_MAXIMO_AUDITORIA));

		if (username != null && !username.isBlank()) {
			Optional<User> usuario = userRepo.findByUsername(username.trim());
			if (usuario.isEmpty()) {
				return new ArrayList<>();
			}
			userId = usuario.get().getId();
		}

		List<AuditLog> eventos = filtrarEventos(userId, action, desde, hasta, tamano);

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
	 * Desactiva un usuario: ya no puede iniciar sesión y se cierran todas sus
	 * sesiones.
	 *
	 * @param adminId id del administrador que hace el cambio
	 * @param userId  id del usuario a desactivar
	 * @param ip      dirección IP desde la que se hace el cambio
	 * @return 0 = desactivado, 1 = no existe, 2 = no puede desactivarse a sí mismo,
	 *         3 = ya estaba desactivado
	 */
	@Transactional
	public int disableUser(Long adminId, Long userId, String ip) {
		Optional<User> found = userRepo.findById(userId);
		if (found.isEmpty()) {
			return 1;
		}
		if (userId.equals(adminId)) {
			return 2;
		}
		User user = found.get();
		if (!user.isEnabled()) {
			return 3;
		}

		user.setEnabled(false);
		userRepo.save(user);
		refreshTokenRepo.revokeAllByUser(user);

		auditService.log(AuditAction.ADMIN_USER_UPDATED, adminId, ip, "desactivó al usuario id=" + userId);
		return 0;
	}

	/**
	 * Activa un usuario y le quita el bloqueo por intentos fallidos si lo tenía.
	 *
	 * @param adminId id del administrador que hace el cambio
	 * @param userId  id del usuario a activar
	 * @param ip      dirección IP desde la que se hace el cambio
	 * @return 0 = activado, 1 = no existe, 2 = ya estaba activo
	 */
	@Transactional
	public int enableUser(Long adminId, Long userId, String ip) {
		Optional<User> found = userRepo.findById(userId);
		if (found.isEmpty()) {
			return 1;
		}
		User user = found.get();
		if (user.isEnabled() && user.getLockedUntil() == null) {
			return 2;
		}

		user.setEnabled(true);
		user.setFailedAttempts(0);
		user.setLockedUntil(null);
		userRepo.save(user);

		auditService.log(AuditAction.ADMIN_USER_UPDATED, adminId, ip, "activó al usuario id=" + userId);
		return 0;
	}

	/**
	 * Devuelve todos los chats con participantes y cantidad de mensajes.
	 *
	 * @return chats ordenados por fecha del último mensaje, del más reciente al más
	 *         antiguo
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
	 * Devuelve los mensajes de un chat descifrados si la clave es correcta. Los
	 * mensajes eliminados por su remitente se muestran con la etiqueta [Eliminado].
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

			String contenido = descifrarSeguro(claveReal, mensaje.getContentEnc());
			if (mensaje.isDeleted()) {
				contenido = "[Eliminado] " + contenido;
			}

			MensajeAdminDTO fila = new MensajeAdminDTO(mensaje.getId(), remitente, tipo,
					contenido, mensaje.getMediaId(), mensaje.getCreatedAt());
			resultado.add(fila);
		}

		return resultado;
	}

	/**
	 * Devuelve la clave de un chat ya descifrada y deja el acceso en la auditoría.
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
	    Optional<Chat> chat = chatRepo.findById(chatId);

	    if (chat.isEmpty()) {
	        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat no encontrado");
	    }

	    return chat.get();
	}

	/** Compara dos claves en tiempo constante; devuelve false si alguna es null. */
	private static boolean clavesIguales(String esperada, String recibida) {
		if (esperada == null || recibida == null) {
			return false;
		}
		return MessageDigest.isEqual(esperada.getBytes(StandardCharsets.UTF_8),
				recibida.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Descifra un mensaje; si falla, devuelve un texto de aviso en vez de romper la
	 * lista.
	 */
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

	/** Busca coincidencias de auditoría desde el evento más reciente. */
	private List<AuditLog> filtrarEventos(Long usuario, AuditAction accion, LocalDateTime desde, LocalDateTime hasta,
			int limite) {

		List<AuditLog> encontrados = new ArrayList<>();
		long antes = Long.MAX_VALUE;

		while (encontrados.size() < limite) {
			List<AuditLog> bloque = auditRepo.findByIdLessThanOrderByIdDesc(antes, PageRequest.of(0, 100));

			for (AuditLog evento : bloque) {
				antes = evento.getId();

				if (usuario != null && !usuario.equals(evento.getUserId())) {
					continue;
				}

				if (accion != null && accion != evento.getAction()) {
					continue;
				}

				if (desde != null && evento.getCreatedAt().isBefore(desde)) {
					continue;
				}

				if (hasta != null && !evento.getCreatedAt().isBefore(hasta)) {
					continue;
				}

				encontrados.add(evento);

				if (encontrados.size() == limite) {
					return encontrados;
				}
			}

			if (bloque.size() < 100) {
				break;
			}
		}

		return encontrados;
	}
}