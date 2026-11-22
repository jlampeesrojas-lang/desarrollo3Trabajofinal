package co.edu.unbosque.trabajofinalback.controller;

import co.edu.unbosque.trabajofinalback.model.Chat;
import co.edu.unbosque.trabajofinalback.model.ChatParticipant;
import co.edu.unbosque.trabajofinalback.model.Media;
import co.edu.unbosque.trabajofinalback.model.Message;
import co.edu.unbosque.trabajofinalback.model.MessageType;
import co.edu.unbosque.trabajofinalback.repository.ChatParticipantRepository;
import co.edu.unbosque.trabajofinalback.repository.MediaRepository;
import co.edu.unbosque.trabajofinalback.repository.MessageRepository;
import co.edu.unbosque.trabajofinalback.security.AuthContext;
import co.edu.unbosque.trabajofinalback.service.ChatCryptoService;
import co.edu.unbosque.trabajofinalback.service.ChatService;
import co.edu.unbosque.trabajofinalback.service.ChatUnlockService;
import co.edu.unbosque.trabajofinalback.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Permite enviar y recuperar adjuntos de una conversación. */
@RestController
@RequestMapping("/media")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Multimedia", description = "Archivos, imágenes y audios de los chats")
public class MediaController {
	private static final long MAX_BYTES = 10L * 1024 * 1024;
	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;
	private final AuthContext auth;
	private final ChatParticipantRepository participantes;
	private final MediaRepository archivos;
	private final MessageRepository mensajes;
	private final ChatCryptoService crypto;
	private final MediaService mediaServ;
	private final ChatService chatServ;
	private final ChatUnlockService permisos;
	private final SecureRandom random;

	/** Recibe los servicios e inicializa el generador para cifrar adjuntos. */
	public MediaController(AuthContext auth, ChatParticipantRepository participantes, MediaRepository archivos,
			MessageRepository mensajes, ChatCryptoService crypto, MediaService mediaServ, ChatService chatServ,
			ChatUnlockService permisos) {

		this.auth = auth;
		this.participantes = participantes;
		this.archivos = archivos;
		this.mensajes = mensajes;
		this.crypto = crypto;
		this.mediaServ = mediaServ;
		this.chatServ = chatServ;
		this.permisos = permisos;
		this.random = new SecureRandom();
	}

	/** Recibe el adjunto enviado desde la barra de mensajes del frontend, con un texto opcional. */
	@Operation(summary = "Enviar un adjunto al chat", description = "Multipart con el campo archivo y, opcional, el campo texto que se muestra"
			+ " en el mismo mensaje. Tamaño máximo: 10 MB.")
	@Transactional
	@PostMapping(path = "/chat/{chatId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> subir(@PathVariable("chatId") Long chatId,
			@RequestParam("archivo") MultipartFile archivo,
			@RequestParam(value = "texto", required = false) String texto) throws IOException {

		Long usuarioId = auth.getUserId();
		Optional<ChatParticipant> encontrado = participantes.findByChatIdAndUserId(chatId, usuarioId);

		if (encontrado.isEmpty()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No participas en este chat.");
		}
		if (!permisos.abierto(chatId)) {
			return ResponseEntity.status(HttpStatus.LOCKED).body("Desbloquea el chat para enviar archivos.");
		}
		if (archivo.isEmpty()) {
			return ResponseEntity.badRequest().body("El archivo está vacío.");
		}
		if (archivo.getSize() > MAX_BYTES) {
			return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body("El archivo supera los 10 MB.");
		}

		String leyenda = texto == null ? "" : texto.trim();
		if (leyenda.length() > 2000) {
			return ResponseEntity.badRequest().body("El texto supera los 2000 caracteres.");
		}

		ChatParticipant participante = encontrado.get();
		Chat chat = participante.getChat();
		String llave = crypto.recuperarLlave(chat.getKeyEnc());

		byte[] contenido = archivo.getBytes();
		String nombre = obtenerNombre(archivo.getOriginalFilename());
		String mime = detectarTipo(contenido, archivo.getContentType());

		MessageType tipo = MessageType.ARCHIVO;
		if (mime.startsWith("image/")) {
			tipo = MessageType.IMAGEN;
		} else if (mime.startsWith("audio/")) {
			tipo = MessageType.AUDIO;
		}

		Media adjunto = new Media(chat, participante.getUser(), mime, contenido.length, cifrar(contenido, llave));
		adjunto = archivos.save(adjunto);

		Message mensaje = new Message(chat, participante.getUser(), tipo, crypto.cifrar(llave, nombre),
				adjunto.getId());
		if (!leyenda.isEmpty()) {
			mensaje.setCaptionEnc(crypto.cifrar(llave, leyenda));
		}
		mensaje = mensajes.save(mensaje);

		chatServ.agregarContactoAlRecibir(chatId, usuarioId);
		chat.setLastMessageAt(mensaje.getCreatedAt());

		return ResponseEntity.status(HttpStatus.CREATED).body("Archivo enviado.");
	}

	/** Recibe una imagen o un audio mediante la ruta que usa MediaService. */
	@Operation(summary = "Enviar imagen o audio a un chat")
	@Transactional
	@PostMapping(path = "/upload/{chatId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> upload(@PathVariable("chatId") Long chatId, @RequestParam("file") MultipartFile file,
			@RequestParam(value = "contenido", required = false) String contenido, HttpServletRequest request) {

		Long usuarioId = auth.getUserId();
		int acceso = chatServ.checkAccess(usuarioId, chatId);

		if (acceso == 1) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El chat no existe.");
		}
		if (acceso == 2) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No participas en este chat.");
		}
		if (!permisos.abierto(chatId)) {
			return ResponseEntity.status(HttpStatus.LOCKED).body("Desbloquea el chat para enviar archivos.");
		}

		int estado = mediaServ.upload(usuarioId, chatId, file, contenido, request.getRemoteAddr());

		if (estado == 0) {
			return ResponseEntity.status(HttpStatus.CREATED).body("Archivo enviado exitosamente");
		}
		if (estado == 1) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El chat no existe.");
		}
		if (estado == 2) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No participas en este chat.");
		}
		if (estado == 3) {
			return ResponseEntity.badRequest().body("El archivo está vacío.");
		}
		if (estado == 4) {
			return ResponseEntity.badRequest().body("Tipo de archivo no permitido. Solo imágenes (JPG, PNG, GIF, WEBP)"
					+ " y audios (MP3, OGG, WEBM, WAV, M4A).");
		}

		return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
				.body("Archivo demasiado grande. Imágenes hasta 5 MB y audios hasta 10 MB.");
	}

	/**
	 * Recupera un adjunto del chat y conserva el acceso administrativo del
	 * proyecto.
	 */
	@Operation(summary = "Descargar un adjunto de un chat")
	@Transactional(readOnly = true)
	@GetMapping("/getbyid/{id}")
	public ResponseEntity<byte[]> descargar(@PathVariable("id") Long id) {
		Optional<Media> encontrado = archivos.findById(id);
		if (encontrado.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		Media adjunto = encontrado.get();
		Chat chat = adjunto.getChat();
		Long chatId = chat.getId();

		boolean participa = participantes.existsByChatIdAndUserId(chatId, auth.getUserId());
		if (participa) {
			if (!permisos.abierto(chatId)) {
				return ResponseEntity.status(HttpStatus.LOCKED).build();
			}
		} else if (!auth.isAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		String llave = crypto.recuperarLlave(chat.getKeyEnc());
		byte[] contenido;

		try {
			contenido = descifrar(adjunto.getDataEnc(), llave);
		} catch (IllegalStateException error) {
			// La ruta /upload usa el formato de almacenamiento de MediaService.
			contenido = mediaServ.descifrar(adjunto);
		}

		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
				.contentType(MediaType.parseMediaType(adjunto.getMimeType())).contentLength(contenido.length)
				.body(contenido);
	}

	/** Obtiene el nombre visible del archivo y elimina la ruta del equipo. */
	private String obtenerNombre(String original) {
		if (original == null || original.isBlank()) {
			return "archivo";
		}

		String nombre = original.replace('\\', '/');
		nombre = nombre.substring(nombre.lastIndexOf('/') + 1);
		nombre = nombre.replaceAll("\\p{Cntrl}", "").trim();

		if (nombre.isEmpty()) {
			return "archivo";
		}
		return nombre.substring(0, Math.min(nombre.length(), 200));
	}

	/** Identifica imágenes, notas de voz y documentos PDF por sus bytes. */
	private String detectarTipo(byte[] datos, String tipoDeclarado) {
		String imagen = detectarImagen(datos);
		if (imagen != null) {
			return imagen;
		}

		String declarado = tipoDeclarado == null ? "" : tipoDeclarado.toLowerCase(Locale.ROOT);
		if (declarado.startsWith("audio/")) {
			String audio = detectarAudio(datos);
			if (audio != null) {
				return audio;
			}
		}

		if (empiezaCon(datos, 0, ascii("%PDF-"))) {
			return "application/pdf";
		}
		return MediaType.APPLICATION_OCTET_STREAM_VALUE;
	}

	/** Identifica imágenes JPEG, PNG, GIF y WEBP. */
	private static String detectarImagen(byte[] datos) {
		if (empiezaCon(datos, 0, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF })) {
			return "image/jpeg";
		}
		if (empiezaCon(datos, 0, new byte[] { (byte) 0x89, 80, 78, 71, 13, 10, 26, 10 })) {
			return "image/png";
		}
		if (empiezaCon(datos, 0, ascii("GIF87a")) || empiezaCon(datos, 0, ascii("GIF89a"))) {
			return "image/gif";
		}
		if (empiezaCon(datos, 0, ascii("RIFF")) && empiezaCon(datos, 8, ascii("WEBP"))) {
			return "image/webp";
		}
		return null;
	}

	/** Identifica las grabaciones WEBM, OGG y MP4. */
	private static String detectarAudio(byte[] datos) {
		if (empiezaCon(datos, 0, new byte[] { 0x1A, 0x45, (byte) 0xDF, (byte) 0xA3 })) {
			return "audio/webm";
		}
		if (empiezaCon(datos, 0, ascii("OggS"))) {
			return "audio/ogg";
		}
		if (datos.length >= 12 && empiezaCon(datos, 4, ascii("ftyp"))) {
			return "audio/mp4";
		}
		return null;
	}

	/** Comprueba si los bytes coinciden con la firma del formato. */
	private static boolean empiezaCon(byte[] datos, int desde, byte[] firma) {
		return datos.length >= desde + firma.length
				&& Arrays.equals(datos, desde, desde + firma.length, firma, 0, firma.length);
	}

	/** Convierte la firma de un formato a bytes ASCII. */
	private static byte[] ascii(String texto) {
		return texto.getBytes(StandardCharsets.US_ASCII);
	}

	/** Cifra el archivo y guarda el vector de inicialización al comienzo. */
	private byte[] cifrar(byte[] contenido, String llave) {
		byte[] iv = new byte[IV_BYTES];
		random.nextBytes(iv);

		byte[] cifrado = transformar(Cipher.ENCRYPT_MODE, contenido, llave, iv);
		return ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
	}

	/**
	 * Recupera los bytes originales de un adjunto guardado por la ruta del chat.
	 */
	private byte[] descifrar(byte[] guardado, String llave) {
		if (guardado == null || guardado.length < IV_BYTES + TAG_BITS / 8) {
			throw new IllegalStateException("El adjunto guardado está incompleto.");
		}

		ByteBuffer buffer = ByteBuffer.wrap(guardado);
		byte[] iv = new byte[IV_BYTES];
		buffer.get(iv);

		byte[] cifrado = new byte[buffer.remaining()];
		buffer.get(cifrado);

		return transformar(Cipher.DECRYPT_MODE, cifrado, llave, iv);
	}

	/** Aplica AES-GCM para cifrar o descifrar los bytes del archivo. */
	private byte[] transformar(int modo, byte[] datos, String llave, byte[] iv) {
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			SecretKeySpec clave = new SecretKeySpec(llave.getBytes(StandardCharsets.UTF_8), "AES");
			cipher.init(modo, clave, new GCMParameterSpec(TAG_BITS, iv));
			return cipher.doFinal(datos);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("No se pudo procesar el adjunto.", e);
		}
	}
}