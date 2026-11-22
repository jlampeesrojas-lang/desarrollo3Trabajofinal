package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.model.PermisoChat;
import co.edu.unbosque.trabajofinalback.repository.PermisoChatRepository;
import co.edu.unbosque.trabajofinalback.security.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administra el desbloqueo temporal de un chat para el usuario autenticado. */
@Service
public class ChatUnlockService {

	private static final long SEGUNDOS_PERMISO = 900;
	private final PermisoChatRepository permisoRepo;
	private final AuthContext auth;
	private final HttpServletRequest peticion;

	public ChatUnlockService(PermisoChatRepository permisoRepo, AuthContext auth, HttpServletRequest peticion) {
		this.permisoRepo = permisoRepo;
		this.auth = auth;
		this.peticion = peticion;
	}

	/** Crea un permiso después de que el controlador valida la clave del chat. */
	@Transactional
	public String abrir(Long chatId) {
		String codigo = UUID.randomUUID().toString();
		Instant venceEn = Instant.now().plusSeconds(SEGUNDOS_PERMISO);

		PermisoChat permiso = new PermisoChat(codigo, auth.getUserId(), chatId, venceEn);

		permisoRepo.save(permiso);
		return codigo;
	}

	/** Lee el permiso que envió la pestaña en la cabecera de la petición. */
	public boolean abierto(Long chatId) {
		String codigo = peticion.getHeader("X-Chat-Permiso");
		return abierto(chatId, codigo);
	}

	/** Comprueba el permiso para este usuario, este chat y el momento actual. */
	public boolean abierto(Long chatId, String codigo) {
		if (codigo == null || codigo.isBlank()) {
			return false;
		}

		return permisoRepo.existsByCodigoAndUsuarioIdAndChatIdAndVenceEnAfter(codigo, auth.getUserId(), chatId,
				Instant.now());
	}

	/** Bloquea el chat para la pestaña que hizo la petición. */
	@Transactional
	public void cerrar(Long chatId) {
		String codigo = peticion.getHeader("X-Chat-Permiso");
		cerrar(chatId, codigo);
	}

	/** Elimina el permiso indicado si pertenece al usuario y al chat. */
	@Transactional
	public void cerrar(Long chatId, String codigo) {
		if (codigo == null || codigo.isBlank()) {
			return;
		}

		permisoRepo.deleteByCodigoAndUsuarioIdAndChatId(codigo, auth.getUserId(), chatId);
	}
}