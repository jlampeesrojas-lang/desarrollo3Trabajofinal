package co.edu.unbosque.trabajofinalback.controller;

import co.edu.unbosque.trabajofinalback.model.Chat;
import co.edu.unbosque.trabajofinalback.model.User;
import co.edu.unbosque.trabajofinalback.repository.ChatParticipantRepository;
import co.edu.unbosque.trabajofinalback.repository.ChatRepository;
import co.edu.unbosque.trabajofinalback.repository.UserRepository;
import co.edu.unbosque.trabajofinalback.security.AuthContext;
import co.edu.unbosque.trabajofinalback.service.ChatCryptoService;
import co.edu.unbosque.trabajofinalback.service.ChatUnlockService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/chats")
public class ChatLockController {

    private final AuthContext auth;
    private final ChatRepository chats;
    private final ChatParticipantRepository participantes;
    private final UserRepository usuarios;
    private final ChatCryptoService crypto;
    private final ChatUnlockService permisos;
    private final PasswordEncoder passwords;

    public ChatLockController(AuthContext auth, ChatRepository chats,
            ChatParticipantRepository participantes, UserRepository usuarios,
            ChatCryptoService crypto, ChatUnlockService permisos,
            PasswordEncoder passwords) {
        this.auth = auth;
        this.chats = chats;
        this.participantes = participantes;
        this.usuarios = usuarios;
        this.crypto = crypto;
        this.permisos = permisos;
        this.passwords = passwords;
    }

    /** Comprueba la contraseña propia antes de mostrar la clave compartida. */
    @PostMapping("/{id}/clave")
    public ResponseEntity<String> verClave(@PathVariable("id") Long id,
            @RequestBody Map<String, String> datos) {
        Long usuarioId = auth.getUserId();

        if (!participantes.existsByChatIdAndUserId(id, usuarioId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String password = datos.get("password");
        User usuario = usuarios.findById(usuarioId).orElseThrow();

        if (password == null || password.isBlank()
                || !passwords.matches(password, usuario.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Contraseña incorrecta.");
        }

        Chat chat = chats.findById(id).orElseThrow();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(crypto.recuperarLlave(chat.getKeyEnc()));
    }

    /** Comprueba la clave y permite leer durante cinco minutos. */
    @PostMapping("/{id}/desbloquear")
    public ResponseEntity<String> desbloquear(@PathVariable("id") Long id,
            @RequestBody Map<String, String> datos) {
        if (!participantes.existsByChatIdAndUserId(id, auth.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String ingresada = datos.get("clave");
        Chat chat = chats.findById(id).orElseThrow();
        String correcta = crypto.recuperarLlave(chat.getKeyEnc());

        if (ingresada == null || !MessageDigest.isEqual(
                correcta.getBytes(StandardCharsets.UTF_8),
                ingresada.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Clave de conversación incorrecta.");
        }

        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(permisos.abrir(id));
    }

    /** Cierra el permiso de esta pestaña. */
    @PostMapping("/{id}/bloquear")
    public ResponseEntity<Void> bloquear(@PathVariable("id") Long id) {
        permisos.cerrar(id);
        return ResponseEntity.noContent().build();
    }
}