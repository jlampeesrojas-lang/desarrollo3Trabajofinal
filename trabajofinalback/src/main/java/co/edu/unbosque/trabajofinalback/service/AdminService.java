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
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminService {

	private final AuditLogRepository auditRepo;
	private final UserRepository userRepo;
	private final ChatRepository chatRepo;
	private final ChatParticipantRepository participantRepo;
	private final MessageRepository messageRepo;
	private final ChatCryptoService crypto;
	private final AuditService auditService;

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

	@Transactional(readOnly = true)
	public List<AuditAdminDTO> audit(int limit) {
		if (limit < 1) {
			limit = 1;
		}
		if (limit > 500) {
			limit = 500;
		}

		Sort orden = Sort.by(Sort.Direction.DESC, "id");
		PageRequest pagina = PageRequest.of(0, limit, orden);
		List<AuditLog> eventos = auditRepo.findAll(pagina).getContent();

		List<Long> ids = new ArrayList<>();
		for (AuditLog evento : eventos) {
			Long id = evento.getUserId();
			if (id != null && !ids.contains(id)) {
				ids.add(id);
			}
		}

		Iterable<User> usuarios = userRepo.findAllById(ids);
		List<AuditAdminDTO> resultado = new ArrayList<>();

		for (AuditLog evento : eventos) {
			String nombre = null;
			for (User usuario : usuarios) {
				if (usuario.getId().equals(evento.getUserId())) {
					nombre = usuario.getUsername();
					break;
				}
			}

			AuditAdminDTO fila = new AuditAdminDTO(evento.getId(), nombre, evento.getAction().name(), evento.getIp(),
					evento.getLocation(), evento.getDetail(), evento.getCreatedAt());
			resultado.add(fila);
		}

		return resultado;
	}

	@Transactional(readOnly = true)
	public List<ChatAdminDTO> chats() {
		List<ChatAdminDTO> resultado = new ArrayList<>();

		for (Chat chat : chatRepo.findAllByOrderByLastMessageAtDesc()) {
			Long chatId = chat.getId();
			List<String> nombres = new ArrayList<>();

			for (ChatParticipant participante : participantRepo.findByChatIdOrderByIdAsc(chatId)) {
				nombres.add(participante.getUser().getUsername());
			}

			ChatAdminDTO fila = new ChatAdminDTO(chatId, chat.getCode(), nombres, chat.getLastMessageAt(),
					messageRepo.countByChatId(chatId));
			resultado.add(fila);
		}

		return resultado;
	}

	@Transactional
	public List<MensajeAdminDTO> messages(Long chatId, String key, Long adminId, String ip) {
		Chat chat = buscarChat(chatId);
		String claveReal = crypto.recuperarLlave(chat.getKeyEnc());

		if (key == null || !MessageDigest.isEqual(claveReal.getBytes(StandardCharsets.UTF_8),
				key.getBytes(StandardCharsets.UTF_8))) {
			throw new InvalidTokenException("Clave incorrecta", HttpStatus.BAD_REQUEST);
		}

		auditService.log(AuditAction.ADMIN_READ_CHAT, adminId, ip, "chat=" + chat.getCode());

		PageRequest pagina = PageRequest.of(0, 200);
		List<Message> mensajes = messageRepo.findByChatIdOrderByIdDesc(chatId, pagina).getContent();
		List<MensajeAdminDTO> resultado = new ArrayList<>();

		for (Message mensaje : mensajes) {
			String remitente = null;
			if (mensaje.getSender() != null) {
				remitente = mensaje.getSender().getUsername();
			}

			String texto = crypto.descifrar(claveReal, mensaje.getContentEnc());
			MensajeAdminDTO fila = new MensajeAdminDTO(mensaje.getId(), remitente, mensaje.getType().name(), texto,
					mensaje.getMediaId(), mensaje.getCreatedAt());
			resultado.add(fila);
		}

		return resultado;
	}

	@Transactional
	public String chatKey(Long chatId, Long adminId, String ip) {
		Chat chat = buscarChat(chatId);
		auditService.log(AuditAction.ADMIN_READ_CHAT, adminId, ip, "clave del chat=" + chat.getCode());
		return crypto.recuperarLlave(chat.getKeyEnc());
	}

	private Chat buscarChat(Long chatId) {
		Chat chat = chatRepo.findById(chatId).orElse(null);
		if (chat == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat no encontrado");
		}
		return chat;
	}
}