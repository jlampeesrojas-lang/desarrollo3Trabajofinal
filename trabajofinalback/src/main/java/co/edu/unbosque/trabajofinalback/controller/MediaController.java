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

    private final SecureRandom random;
    private final AuthContext auth;
    private final ChatParticipantRepository participantes;
    private final MediaRepository archivos;
    private final MessageRepository mensajes;
    private final ChatCryptoService crypto;
    private final MediaService mediaServ;

    /** Recibe las dependencias e inicializa el generador usado al cifrar archivos. */
    public MediaController(
            AuthContext auth,
            ChatParticipantRepository participantes,
            MediaRepository archivos,
            MessageRepository mensajes,
            ChatCryptoService crypto,
            MediaService mediaServ) {

        this.auth = auth;
        this.participantes = participantes;
        this.archivos = archivos;
        this.mensajes = mensajes;
        this.crypto = crypto;
        this.mediaServ = mediaServ;
        this.random = new SecureRandom();
    }

    /** Recibe el adjunto enviado desde la barra de mensajes del frontend. */
    @Operation(summary = "Enviar un adjunto al chat",
            description = "Multipart con el campo archivo. Tamaño máximo: 10 MB.")
    @Transactional
    @PostMapping(path = "/chat/{chatId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> subir(
            @PathVariable("chatId") Long chatId,
            @RequestParam("archivo") MultipartFile archivo) throws IOException {

        Optional<ChatParticipant> encontrado =
                participantes.findByChatIdAndUserId(chatId, auth.getUserId());

        if (encontrado.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("No participas en este chat.");
        }
        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().body("El archivo está vacío.");
        }
        if (archivo.getSize() > MAX_BYTES) {
            return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                    .body("El archivo supera los 10 MB.");
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

        Media adjunto = new Media(
                chat, participante.getUser(), mime, contenido.length, cifrar(contenido, llave));
        adjunto = archivos.save(adjunto);

        Message mensaje = new Message(
                chat, participante.getUser(), tipo, crypto.cifrar(llave, nombre), adjunto.getId());
        mensaje = mensajes.save(mensaje);
        chat.setLastMessageAt(mensaje.getCreatedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body("Archivo enviado.");
    }

    /** Recibe una imagen o un audio mediante la ruta que usa MediaService. */
    @Operation(summary = "Enviar imagen o audio a un chat",
            description = "Multipart con file y un campo contenido opcional.")
    @Transactional
    @PostMapping(path = "/upload/{chatId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(
            @PathVariable("chatId") Long chatId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "contenido", required = false) String contenido,
            HttpServletRequest request) {

        int status = mediaServ.upload(
                auth.getUserId(), chatId, file, contenido, request.getRemoteAddr());

        if (status == 0) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Archivo enviado exitosamente");
        }
        if (status == 1) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El chat no existe");
        }
        if (status == 2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("No participas en este chat");
        }
        if (status == 3) {
            return ResponseEntity.badRequest().body("El archivo está vacío");
        }
        if (status == 4) {
            return ResponseEntity.badRequest().body(
                    "Tipo de archivo no permitido. Solo imágenes (JPG, PNG, GIF, WEBP)"
                            + " y audios (MP3, OGG, WEBM, WAV, M4A)");
        }
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body("Archivo demasiado grande. Imágenes hasta 5 MB y audios hasta 10 MB");
    }

    /** Descarga un adjunto para un participante del chat o un administrador. */
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

        if (!auth.isAdmin()
                && !participantes.existsByChatIdAndUserId(chat.getId(), auth.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String llave = crypto.recuperarLlave(chat.getKeyEnc());
        byte[] contenido;

        try {
            contenido = descifrar(adjunto.getDataEnc(), llave);
        } catch (IllegalStateException error) {
            // Un adjunto escrito por la otra ruta puede usar el formato de MediaService.
            contenido = mediaServ.descifrar(adjunto);
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(adjunto.getMimeType()))
                .contentLength(contenido.length)
                .body(contenido);
    }

    /** Obtiene el nombre visible y elimina cualquier ruta del equipo del usuario. */
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

    /** Reconoce las imágenes, grabaciones y PDF que llegan desde el chat. */
    private String detectarTipo(byte[] datos, String tipoDeclarado) {
        if (datos.length >= 3
                && datos[0] == (byte) 0xFF
                && datos[1] == (byte) 0xD8
                && datos[2] == (byte) 0xFF) {
            return "image/jpeg";
        }

        byte[] firmaPng = {(byte) 0x89, 80, 78, 71, 13, 10, 26, 10};
        if (datos.length >= firmaPng.length
                && Arrays.equals(datos, 0, 8, firmaPng, 0, 8)) {
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

        String declarado = "";
        if (tipoDeclarado != null) {
            declarado = tipoDeclarado.toLowerCase(Locale.ROOT);
        }

        if (declarado.startsWith("audio/") && datos.length >= 4) {
            boolean webm = datos[0] == (byte) 0x1A
                    && datos[1] == (byte) 0x45
                    && datos[2] == (byte) 0xDF
                    && datos[3] == (byte) 0xA3;

            if (webm) {
                return "audio/webm";
            }

            String inicio = new String(datos, 0, 4, StandardCharsets.US_ASCII);
            if (inicio.equals("OggS")) {
                return "audio/ogg";
            }

            if (datos.length >= 12) {
                String marca = new String(datos, 4, 4, StandardCharsets.US_ASCII);
                if (marca.equals("ftyp")) {
                    return "audio/mp4";
                }
            }
        }

        if (datos.length >= 5) {
            String firma = new String(datos, 0, 5, StandardCharsets.US_ASCII);
            if (firma.equals("%PDF-")) {
                return "application/pdf";
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    /** Cifra el archivo y antepone el vector de inicialización a los bytes cifrados. */
    private byte[] cifrar(byte[] contenido, String llave) {
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);

        byte[] cifrado = transformar(Cipher.ENCRYPT_MODE, contenido, llave, iv);
        return ByteBuffer.allocate(iv.length + cifrado.length)
                .put(iv)
                .put(cifrado)
                .array();
    }

    /** Recupera los bytes originales de un archivo guardado por la ruta del chat. */
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

    /** Aplica AES-GCM para cifrar o descifrar el contenido. */
    private byte[] transformar(int modo, byte[] datos, String llave, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec clave =
                    new SecretKeySpec(llave.getBytes(StandardCharsets.UTF_8), "AES");

            cipher.init(modo, clave, new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(datos);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo procesar el adjunto.", e);
        }
    }
}