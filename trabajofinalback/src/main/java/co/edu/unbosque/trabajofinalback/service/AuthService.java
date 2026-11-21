package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.dto.AuthResponseDTO;
import co.edu.unbosque.trabajofinalback.dto.LoginDTO;
import co.edu.unbosque.trabajofinalback.dto.RegisterDTO;
import co.edu.unbosque.trabajofinalback.exception.EmailNotVerifiedException;
import co.edu.unbosque.trabajofinalback.exception.InvalidTokenException;
import co.edu.unbosque.trabajofinalback.exception.PasswordNotValidException;
import co.edu.unbosque.trabajofinalback.exception.UserAlreadyExistsException;
import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.model.EmailToken;
import co.edu.unbosque.trabajofinalback.model.EmailToken.TokenType;
import co.edu.unbosque.trabajofinalback.model.RefreshToken;
import co.edu.unbosque.trabajofinalback.model.User;
import co.edu.unbosque.trabajofinalback.repository.EmailTokenRepository;
import co.edu.unbosque.trabajofinalback.repository.RefreshTokenRepository;
import co.edu.unbosque.trabajofinalback.repository.UserRepository;
import co.edu.unbosque.trabajofinalback.security.JwtUtil;
import co.edu.unbosque.trabajofinalback.util.PasswordValidator;
import co.edu.unbosque.trabajofinalback.util.TokenUtil;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro, confirmación de correo, recuperación de contraseña y manejo de sesiones. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Minutos de validez del código de recuperación de contraseña. */
    private static final int MINUTOS_CODIGO_RECUPERACION = 15;

    /** Límite superior (exclusivo) del código de 6 dígitos. */
    private static final int LIMITE_CODIGO = 1_000_000;

    /** Intentos fallidos seguidos antes de bloquear la cuenta. */
    private static final int MAX_INTENTOS_FALLIDOS = 5;

    /** Minutos que dura el bloqueo de la cuenta. */
    private static final int MINUTOS_BLOQUEO = 15;

    private final UserRepository userRepo;
    private final EmailTokenRepository emailTokenRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${app.email-token-expiration-hours:24}")
    private long emailTokenHours;

    @Value("${jwt.refresh-expiration-days:7}")
    private long refreshDays;

    /**
     * Crea el servicio con sus repositorios y utilidades.
     *
     * @param userRepo              repositorio de usuarios
     * @param emailTokenRepo        repositorio de tokens enviados por correo
     * @param refreshTokenRepo      repositorio de refresh tokens
     * @param passwordEncoder       codificador de contraseñas
     * @param authenticationManager gestor de autenticación de Spring Security
     * @param jwtUtil               utilidad para generar JWT
     * @param emailService          servicio de envío de correos
     * @param auditService          servicio para registrar auditoría
     */
    public AuthService(UserRepository userRepo,
                       EmailTokenRepository emailTokenRepo,
                       RefreshTokenRepository refreshTokenRepo,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       EmailService emailService,
                       AuditService auditService) {
        this.userRepo = userRepo;
        this.emailTokenRepo = emailTokenRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    /**
     * Registra un usuario con rol USER y envía el correo de confirmación.
     *
     * @param dto datos de registro
     * @param ip  dirección IP del cliente
     * @throws PasswordNotValidException si la contraseña no cumple la política
     */
    @Transactional
    public void register(RegisterDTO dto, String ip) throws PasswordNotValidException {
        PasswordValidator.validate(dto.getPassword());

        String username = dto.getUsername().trim();
        String email = normalizarCorreo(dto.getEmail());

        if (userRepo.existsByUsername(username)) {
            throw new UserAlreadyExistsException("El nombre de usuario ya existe");
        }

        if (userRepo.existsByEmail(email)) {
            throw new UserAlreadyExistsException("El correo ya está registrado");
        }

        String passwordHash = passwordEncoder.encode(dto.getPassword());
        User user = new User(username, email, passwordHash, User.Role.USER);

        userRepo.save(user);
        auditService.log(AuditAction.REGISTER, user.getId(), ip, "email=" + email);
        sendConfirmation(user);
    }

    /** Crea un token de confirmación y lo envía por correo. */
    private void sendConfirmation(User user) {
        String tokenOriginal = TokenUtil.generateToken();
        String hash = TokenUtil.sha256(tokenOriginal);
        LocalDateTime vencimiento = LocalDateTime.now().plusHours(emailTokenHours);

        EmailToken token = new EmailToken(user, hash, TokenType.CONFIRM_EMAIL, vencimiento);
        emailTokenRepo.save(token);

        try {
            emailService.sendConfirmationEmail(user, tokenOriginal);
        } catch (MailException e) {
            log.error("No se pudo enviar el correo de confirmación a {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * Confirma el correo con el token recibido en el enlace.
     *
     * @param rawToken token recibido en el enlace
     * @param ip       dirección IP del cliente
     * @throws InvalidTokenException si el token no existe, está usado o venció
     */
    @Transactional
    public void confirmEmail(String rawToken, String ip) {
        if (estaVacio(rawToken)) {
            throw new InvalidTokenException("El enlace de confirmación no es válido", HttpStatus.BAD_REQUEST);
        }

        String hash = TokenUtil.sha256(rawToken);
        EmailToken token = emailTokenRepo.findByTokenHash(hash).orElse(null);

        if (token == null || token.getType() != TokenType.CONFIRM_EMAIL) {
            throw new InvalidTokenException("El enlace de confirmación no es válido", HttpStatus.BAD_REQUEST);
        }

        if (token.isUsed() || token.isExpired()) {
            throw new InvalidTokenException("El enlace ya fue usado o venció", HttpStatus.BAD_REQUEST);
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        token.setUsedAt(LocalDateTime.now());

        userRepo.save(user);
        emailTokenRepo.save(token);
        auditService.log(AuditAction.EMAIL_CONFIRMED, user.getId(), ip, null);
    }

    /**
     * Reenvía el correo de confirmación sin revelar si el correo existe.
     *
     * @param email correo de la cuenta
     * @param ip    dirección IP del cliente
     */
    @Transactional
    public void resendConfirmation(String email, String ip) {
        if (estaVacio(email)) {
            return;
        }

        User user = userRepo.findByEmail(normalizarCorreo(email)).orElse(null);

        if (user == null || user.isEmailVerified()) {
            return;
        }

        invalidarPendientes(user, TokenType.CONFIRM_EMAIL, LocalDateTime.now());
        sendConfirmation(user);
        auditService.log(AuditAction.CONFIRMATION_RESENT, user.getId(), ip, null);
    }

    /**
     * Calcula el hash del código de recuperación usando también el correo.
     * Se usa al guardar el código y al comprobar el que escribe el usuario.
     */
    private String resetHash(String email, String code) {
        return TokenUtil.sha256(normalizarCorreo(email) + ":" + code.trim());
    }

    /**
     * Genera un código de 6 dígitos y lo envía al correo de la cuenta.
     * No revela si el correo existe.
     *
     * @param email correo de la cuenta
     * @param ip    dirección IP del cliente
     */
    @Transactional
    public void requestPasswordReset(String email, String ip) {
        if (estaVacio(email)) {
            return;
        }

        String correo = normalizarCorreo(email);
        User user = userRepo.findByEmail(correo).orElse(null);

        if (user == null || !user.isEmailVerified()) {
            return;
        }

        invalidarPendientes(user, TokenType.RESET_PASSWORD, LocalDateTime.now());

        // %06d conserva los ceros iniciales de códigos como 002731.
        String codigo = String.format("%06d", RANDOM.nextInt(LIMITE_CODIGO));
        String hash = resetHash(correo, codigo);
        LocalDateTime vencimiento = LocalDateTime.now().plusMinutes(MINUTOS_CODIGO_RECUPERACION);

        EmailToken token = new EmailToken(user, hash, TokenType.RESET_PASSWORD, vencimiento);
        emailTokenRepo.save(token);

        try {
            emailService.sendResetCodeEmail(user, codigo);
        } catch (MailException e) {
            log.error("No se pudo enviar el código de recuperación a {}: {}", correo, e.getMessage());
        }

        auditService.log(AuditAction.PASSWORD_RESET_REQUESTED, user.getId(), ip, null);
    }

    /** Busca el código de recuperación y revisa que no esté usado ni vencido. */
    private EmailToken validResetToken(String email, String code) {
        if (estaVacio(email) || estaVacio(code)) {
            throw new InvalidTokenException("Código incorrecto o vencido", HttpStatus.BAD_REQUEST);
        }

        String hash = resetHash(email, code);
        EmailToken token = emailTokenRepo.findByTokenHash(hash).orElse(null);

        if (token == null || token.getType() != TokenType.RESET_PASSWORD
                || token.isUsed() || token.isExpired()) {
            throw new InvalidTokenException("Código incorrecto o vencido", HttpStatus.BAD_REQUEST);
        }

        return token;
    }

    /**
     * Comprueba el código de recuperación sin gastarlo.
     *
     * @param email correo de la cuenta
     * @param code  código de 6 dígitos
     * @throws InvalidTokenException si el código es incorrecto o venció
     */
    @Transactional(readOnly = true)
    public void verifyResetCode(String email, String code) {
        validResetToken(email, code);
    }

    /**
     * Cambia la contraseña con el código y cierra todas las sesiones.
     *
     * @param email       correo de la cuenta
     * @param code        código de 6 dígitos
     * @param newPassword nueva contraseña
     * @param ip          dirección IP del cliente
     * @throws PasswordNotValidException si la nueva contraseña no cumple la política
     */
    @Transactional
    public void resetPassword(String email, String code, String newPassword, String ip)
            throws PasswordNotValidException {
        EmailToken token = validResetToken(email, code);
        PasswordValidator.validate(newPassword);

        User user = token.getUser();
        Long userId = user.getId();

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.setUsedAt(LocalDateTime.now());

        userRepo.save(user);
        emailTokenRepo.save(token);
        refreshTokenRepo.revokeAllByUser(user);
        auditService.log(AuditAction.PASSWORD_RESET_DONE, userId, ip, null);
    }

    /**
     * Autentica con correo o usuario y entrega los tokens de sesión. Tras 5 intentos fallidos
     * seguidos la cuenta queda bloqueada 15 minutos. No revierte la transacción ante errores de
     * autenticación para que el intento fallido quede guardado.
     *
     * @param dto credenciales de acceso
     * @param ip  dirección IP del cliente
     * @return tokens de sesión y datos básicos del usuario
     * @throws LockedException            si la cuenta está bloqueada
     * @throws AuthenticationException    si las credenciales son incorrectas
     * @throws EmailNotVerifiedException  si el correo no está confirmado
     */
    @Transactional(noRollbackFor = AuthenticationException.class)
    public AuthResponseDTO login(LoginDTO dto, String ip) {
        String identifier = dto.getIdentifier().trim();
        Authentication authentication;

        try {
            UsernamePasswordAuthenticationToken credenciales =
                new UsernamePasswordAuthenticationToken(identifier, dto.getPassword());

            authentication = authenticationManager.authenticate(credenciales);
        } catch (LockedException e) {
            auditService.log(AuditAction.LOGIN_FAIL, buscarIdUsuario(identifier), ip, "cuenta bloqueada");
            throw e;
        } catch (BadCredentialsException e) {
            registrarIntentoFallido(identifier, ip);
            throw e;
        } catch (AuthenticationException e) {
            auditService.log(AuditAction.LOGIN_FAIL, buscarIdUsuario(identifier), ip, "cuenta no disponible");
            throw e;
        }

        User user = (User) authentication.getPrincipal();

        if (!user.isEmailVerified()) {
            auditService.log(AuditAction.LOGIN_FAIL, user.getId(), ip, "correo sin confirmar");
            throw new EmailNotVerifiedException();
        }

        if (user.getFailedAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            userRepo.save(user);
        }

        auditService.log(AuditAction.LOGIN_OK, user.getId(), ip, null);
        return buildSession(user);
    }

    /**
     * Suma un intento fallido al usuario y lo bloquea si llega al máximo. Si un bloqueo anterior ya
     * venció, el conteo empieza de nuevo.
     *
     * @param identifier nombre de usuario o correo escrito en el login
     * @param ip         dirección IP del cliente
     */
    private void registrarIntentoFallido(String identifier, String ip) {
        User user = buscarUsuario(identifier);
        if (user == null) {
            auditService.log(AuditAction.LOGIN_FAIL, null, ip, "credenciales incorrectas");
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isBefore(ahora)) {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
        }

        int intentos = user.getFailedAttempts() + 1;
        user.setFailedAttempts(intentos);
        auditService.log(AuditAction.LOGIN_FAIL, user.getId(), ip,
                "credenciales incorrectas (intento " + intentos + " de " + MAX_INTENTOS_FALLIDOS + ")");

        if (intentos >= MAX_INTENTOS_FALLIDOS) {
            user.setLockedUntil(ahora.plusMinutes(MINUTOS_BLOQUEO));
            auditService.log(AuditAction.ACCOUNT_LOCKED, user.getId(), ip,
                    "bloqueada por " + MINUTOS_BLOQUEO + " minutos");
        }

        userRepo.save(user);
    }

    /** Busca por nombre de usuario y, si hace falta, por correo. */
    private User buscarUsuario(String identifier) {
        User usuario = userRepo.findByUsername(identifier).orElse(null);

        if (usuario == null) {
            String correo = identifier.toLowerCase(Locale.ROOT);
            usuario = userRepo.findByEmail(correo).orElse(null);
        }

        return usuario;
    }

    /** Busca el id de un usuario por nombre de usuario o correo; devuelve null si no existe. */
    private Long buscarIdUsuario(String identifier) {
        User user = buscarUsuario(identifier);
        return user == null ? null : user.getId();
    }

    /** Genera el access token y un refresh token nuevo. */
    private AuthResponseDTO buildSession(User user) {
        String accessToken = jwtUtil.generateToken(user);
        String refreshOriginal = TokenUtil.generateToken();
        String hash = TokenUtil.sha256(refreshOriginal);
        LocalDateTime vencimiento = LocalDateTime.now().plusDays(refreshDays);

        RefreshToken refresh = new RefreshToken(user, hash, vencimiento);
        refreshTokenRepo.save(refresh);

        return new AuthResponseDTO(
            accessToken,
            refreshOriginal,
            jwtUtil.getAccessExpirationSeconds(),
            user.getUsername(),
            user.getRole().name()
        );
    }

    /**
     * Renueva la sesión con rotación del refresh token.
     * Si detecta que se reutiliza un token ya revocado, cierra todas las sesiones del usuario.
     *
     * @param rawRefreshToken refresh token recibido
     * @param ip              dirección IP del cliente
     * @return nuevos tokens de sesión
     * @throws InvalidTokenException si el token es inválido, fue reutilizado o venció
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResponseDTO refresh(String rawRefreshToken, String ip) {
        if (estaVacio(rawRefreshToken)) {
            throw new InvalidTokenException("Refresh token inválido", HttpStatus.UNAUTHORIZED);
        }

        String hash = TokenUtil.sha256(rawRefreshToken);
        RefreshToken token = refreshTokenRepo.findByTokenHash(hash).orElse(null);

        if (token == null) {
            throw new InvalidTokenException("Refresh token inválido", HttpStatus.UNAUTHORIZED);
        }

        User user = token.getUser();

        if (token.isRevoked()) {
            refreshTokenRepo.revokeAllByUser(user);
            auditService.log(AuditAction.TOKEN_REUSE_DETECTED, user.getId(), ip, "refresh token reutilizado");
            throw new InvalidTokenException("Sesión inválida, inicia sesión de nuevo", HttpStatus.UNAUTHORIZED);
        }

        if (token.isExpired() || !user.isEnabled() || !user.isAccountNonLocked() || !user.isEmailVerified()) {
            throw new InvalidTokenException("La sesión venció, inicia sesión de nuevo", HttpStatus.UNAUTHORIZED);
        }

        token.setRevoked(true);
        refreshTokenRepo.save(token);
        auditService.log(AuditAction.TOKEN_REFRESH, user.getId(), ip, null);

        return buildSession(user);
    }

    /**
     * Cierra la sesión revocando el refresh token.
     *
     * @param rawRefreshToken refresh token a revocar
     * @param ip              dirección IP del cliente
     */
    @Transactional
    public void logout(String rawRefreshToken, String ip) {
        if (estaVacio(rawRefreshToken)) {
            return;
        }

        String hash = TokenUtil.sha256(rawRefreshToken);
        RefreshToken token = refreshTokenRepo.findByTokenHash(hash).orElse(null);

        if (token == null || token.isRevoked()) {
            return;
        }

        token.setRevoked(true);
        refreshTokenRepo.save(token);
        auditService.log(AuditAction.LOGOUT, token.getUser().getId(), ip, null);
    }

    /** Pasa el correo a minúsculas y sin espacios, igual en todo el servicio. */
    private static String normalizarCorreo(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** Indica si un texto es null o solo tiene espacios. */
    private static boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }
    
    /** Marca como usados los tokens pendientes del usuario y del tipo indicado. */
    private void invalidarPendientes(User user, TokenType type, LocalDateTime ahora) {
        for (EmailToken token : emailTokenRepo.findByUserAndTypeAndUsedAtIsNull(user, type)) {
            token.setUsedAt(ahora);
        }

        emailTokenRepo.flush();
    }
}