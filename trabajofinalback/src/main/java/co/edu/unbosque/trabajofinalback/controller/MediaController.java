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

/** Permite a los participantes de un chat enviar y recuperar adjuntos. */
@RestController
@RequestMapping("/media")
public class MediaController {

	private static final long MAX_BYTES = 10L * 1024 * 1024;
	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;
	private final SecureRandom random;
	private final AuthContext auth;
	private final ChatParticipantRepository participantes;
	private final MediaRepository archivos;
	private final MessageRepository mensajes;
	private final ChatCryptoService crypto;

	/** Recibe los servicios y repositorios necesarios para manejar adjuntos. */
	public MediaController(AuthContext auth, ChatParticipantRepository participantes, MediaRepository archivos,
			MessageRepository mensajes, ChatCryptoService crypto) {
		this.auth = auth;
		this.participantes = participantes;
		this.archivos = archivos;
		this.mensajes = mensajes;
		this.crypto = crypto;
		this.random = new SecureRandom();
	}

	/** Guarda el archivo cifrado y agrega su mensaje al historial del chat. */
	@Transactional
	@PostMapping(value = "/chat/{chatId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> subir(@PathVariable("chatId") Long chatId,
			@RequestParam("archivo") MultipartFile archivo) throws IOException {

		Optional<ChatParticipant> encontrado = participantes.findByChatIdAndUserId(chatId, auth.getUserId());

		if (encontrado.isEmpty()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No participas en este chat.");
		}

		if (archivo.isEmpty()) {
			return ResponseEntity.badRequest().body("El archivo está vacío.");
		}

		if (archivo.getSize() > MAX_BYTES) {
			return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body("El archivo supera los 10 MB.");
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
		mensaje = mensajes.save(mensaje);

		chat.setLastMessageAt(mensaje.getCreatedAt());

		return ResponseEntity.status(HttpStatus.CREATED).body("Archivo enviado.");
	}

	/** Devuelve el adjunto si el usuario pertenece a la conversación. */
	@Transactional(readOnly = true)
	@GetMapping("/getbyid/{id}")
	public ResponseEntity<byte[]> descargar(@PathVariable("id") Long id) {

		Optional<Media> encontrado = archivos.findById(id);

		if (encontrado.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		Media adjunto = encontrado.get();
		Chat chat = adjunto.getChat();

		if (!participantes.existsByChatIdAndUserId(chat.getId(), auth.getUserId())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		String llave = crypto.recuperarLlave(chat.getKeyEnc());
		byte[] contenido = descifrar(adjunto.getDataEnc(), llave);

		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
				.contentType(MediaType.parseMediaType(adjunto.getMimeType())).body(contenido);
	}

	/** Obtiene el nombre que se mostrará en la burbuja del chat. */
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

	/** Reconoce los formatos que el front puede mostrar dentro del chat. */
	private String detectarTipo(byte[] datos, String tipoDeclarado) {
		if (datos.length >= 3 && datos[0] == (byte) 0xFF && datos[1] == (byte) 0xD8 && datos[2] == (byte) 0xFF) {
			return "image/jpeg";
		}

		byte[] firmaPng = { (byte) 0x89, 80, 78, 71, 13, 10, 26, 10 };
		if (datos.length >= firmaPng.length && Arrays.equals(datos, 0, 8, firmaPng, 0, 8)) {
			return "image/png";
		}

		if (datos.length >= 6) {
			String firma = new String(datos, 0, 6, StandardCharsets.US_ASCII);
			if (firma.equals("GIF87a") || firma.equals("GIF89a")) {
				return "image/gif";
			}
		}

		if (datos.length >= 12) {
			String inicio = new String(datos, 0, 4, StandardCharsets.US_ASCII);
			String formato = new String(datos, 8, 4, StandardCharsets.US_ASCII);
			if (inicio.equals("RIFF") && formato.equals("WEBP")) {
				return "image/webp";
			}
		}

		String declarado = tipoDeclarado == null ? "" : tipoDeclarado.toLowerCase(Locale.ROOT);

		if (declarado.startsWith("audio/") && datos.length >= 4) {
			boolean webm = datos[0] == (byte) 0x1A && datos[1] == (byte) 0x45 && datos[2] == (byte) 0xDF
					&& datos[3] == (byte) 0xA3;

			String inicio = new String(datos, 0, 4, StandardCharsets.US_ASCII);

			if (webm)
				return "audio/webm";
			if (inicio.equals("OggS"))
				return "audio/ogg";

			if (datos.length >= 12) {
				String marca = new String(datos, 4, 4, StandardCharsets.US_ASCII);
				if (marca.equals("ftyp"))
					return "audio/mp4";
			}
		}

		if (datos.length >= 5) {
			String inicio = new String(datos, 0, 5, StandardCharsets.US_ASCII);
			if (inicio.equals("%PDF-"))
				return "application/pdf";
		}
		return MediaType.APPLICATION_OCTET_STREAM_VALUE;
	}

	/** Cifra los bytes y guarda el vector de inicialización al comienzo. */
	private byte[] cifrar(byte[] contenido, String llave) {
		byte[] iv = new byte[IV_BYTES];
		random.nextBytes(iv);

		byte[] cifrado = transformar(Cipher.ENCRYPT_MODE, contenido, llave, iv);

		return ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
	}

	/** Lee el vector de inicialización y recupera los bytes originales. */
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

	/** Aplica AES-GCM con la llave de la conversación. */
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