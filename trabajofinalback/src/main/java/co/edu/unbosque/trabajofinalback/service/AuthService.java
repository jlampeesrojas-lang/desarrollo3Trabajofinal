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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** AuthService. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom random = new SecureRandom();

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

    /** Crea el servicio con sus repositorios y utilidades. */
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

    /** Registra un usuario con rol USER y envía el correo de confirmación. */
    @Transactional
    public void register(RegisterDTO dto, String ip) throws PasswordNotValidException {
        PasswordValidator.validate(dto.getPassword());

        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim().toLowerCase();

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
            log.error("No se pudo enviar el correo de confirmación a {}", user.getEmail());
        }
    }

    /** Confirma el correo con el token recibido en el enlace. */
    @Transactional
    public void confirmEmail(String rawToken, String ip) {
        String hash = TokenUtil.sha256(rawToken);
        EmailToken token = emailTokenRepo.findByTokenHash(hash).orElse(null);

        if (token == null || token.getType() != TokenType.CONFIRM_EMAIL) {
            throw new InvalidTokenException(
                "El enlace de confirmación no es válido", HttpStatus.BAD_REQUEST
            );
        }

        if (token.isUsed() || token.isExpired()) {
            throw new InvalidTokenException(
                "El enlace ya fue usado o venció", HttpStatus.BAD_REQUEST
            );
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        token.setUsedAt(LocalDateTime.now());

        userRepo.save(user);
        emailTokenRepo.save(token);
        auditService.log(AuditAction.EMAIL_CONFIRMED, user.getId(), ip, null);
    }

    /** Reenvía el correo de confirmación sin revelar si el correo existe. */
    @Transactional
    public void resendConfirmation(String email, String ip) {
        String correo = email.trim().toLowerCase();
        User user = userRepo.findByEmail(correo).orElse(null);

        if (user == null || user.isEmailVerified()) {
            return;
        }

        emailTokenRepo.invalidatePending(user, TokenType.CONFIRM_EMAIL, LocalDateTime.now());
        sendConfirmation(user);
        auditService.log(AuditAction.CONFIRMATION_RESENT, user.getId(), ip, null);
    }

    // Se usa al guardar el código y al comprobar el que escribe el usuario.
    /** Calcula el hash del código de recuperación usando también el correo. */
    private String resetHash(String email, String code) {
        String correo = email.trim().toLowerCase();
        return TokenUtil.sha256(correo + ":" + code.trim());
    }

    /** Genera un código de 6 dígitos y lo envía al correo de la cuenta. */
    @Transactional
    public void requestPasswordReset(String email, String ip) {
        String correo = email.trim().toLowerCase();
        User user = userRepo.findByEmail(correo).orElse(null);

        if (user == null || !user.isEmailVerified()) {
            return;
        }

        emailTokenRepo.invalidatePending(user, TokenType.RESET_PASSWORD, LocalDateTime.now());

        // %06d conserva los ceros iniciales de códigos como 002731.
        String codigo = String.format("%06d", random.nextInt(1_000_000));
        String hash = resetHash(correo, codigo);
        LocalDateTime vencimiento = LocalDateTime.now().plusMinutes(15);

        EmailToken token = new EmailToken(user, hash, TokenType.RESET_PASSWORD, vencimiento);
        emailTokenRepo.save(token);

        try {
            emailService.sendResetCodeEmail(user, codigo);
        } catch (MailException e) {
            log.error("No se pudo enviar el código de recuperación a {}", correo);
        }

        auditService.log(AuditAction.PASSWORD_RESET_REQUESTED, user.getId(), ip, null);
    }

    /** Busca el código de recuperación y revisa que no esté usado ni vencido. */
    private EmailToken validResetToken(String email, String code) {
        String hash = resetHash(email, code);
        EmailToken token = emailTokenRepo.findByTokenHash(hash).orElse(null);

        if (token == null || token.getType() != TokenType.RESET_PASSWORD
                || token.isUsed() || token.isExpired()) {
            throw new InvalidTokenException("Código incorrecto o vencido", HttpStatus.BAD_REQUEST);
        }

        return token;
    }

    /** Comprueba el código de recuperación sin gastarlo. */
    @Transactional(readOnly = true)
    public void verifyResetCode(String email, String code) {
        validResetToken(email, code);
    }

    /** Cambia la contraseña con el código y cierra todas las sesiones. */
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

    /** Autentica con correo o usuario y entrega los tokens de sesión. */
    @Transactional
    public AuthResponseDTO login(LoginDTO dto, String ip) {
        String identifier = dto.getIdentifier().trim();
        Authentication authentication;

        try {
            UsernamePasswordAuthenticationToken credenciales =
                new UsernamePasswordAuthenticationToken(identifier, dto.getPassword());

            authentication = authenticationManager.authenticate(credenciales);
        } catch (AuthenticationException e) {
            User user = userRepo.findByUsername(identifier).orElse(null);

            if (user == null) {
                user = userRepo.findByEmail(identifier.toLowerCase()).orElse(null);
            }

            Long userId = null;
            if (user != null) {
                userId = user.getId();
            }

            auditService.log(AuditAction.LOGIN_FAIL, userId, ip, "credenciales incorrectas");
            throw e;
        }

        User user = (User) authentication.getPrincipal();

        if (!user.isEmailVerified()) {
            auditService.log(AuditAction.LOGIN_FAIL, user.getId(), ip, "correo sin confirmar");
            throw new EmailNotVerifiedException();
        }

        auditService.log(AuditAction.LOGIN_OK, user.getId(), ip, null);
        return buildSession(user);
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

    /** Renueva la sesión con rotación del refresh token. */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResponseDTO refresh(String rawRefreshToken, String ip) {
        String hash = TokenUtil.sha256(rawRefreshToken);
        RefreshToken token = refreshTokenRepo.findByTokenHash(hash).orElse(null);

        if (token == null) {
            throw new InvalidTokenException("Refresh token inválido", HttpStatus.UNAUTHORIZED);
        }

        User user = token.getUser();

        if (token.isRevoked()) {
            Long userId = user.getId();
            refreshTokenRepo.revokeAllByUser(user);
            auditService.log(AuditAction.TOKEN_REUSE_DETECTED, userId, ip, "refresh token reutilizado");

            throw new InvalidTokenException(
                "Sesión inválida, inicia sesión de nuevo", HttpStatus.UNAUTHORIZED
            );
        }

        if (token.isExpired() || !user.isEnabled() || !user.isEmailVerified()) {
            throw new InvalidTokenException(
                "La sesión venció, inicia sesión de nuevo", HttpStatus.UNAUTHORIZED
            );
        }

        token.setRevoked(true);
        refreshTokenRepo.save(token);
        auditService.log(AuditAction.TOKEN_REFRESH, user.getId(), ip, null);

        return buildSession(user);
    }

    /** Cierra la sesión revocando el refresh token. */
    @Transactional
    public void logout(String rawRefreshToken, String ip) {
        String hash = TokenUtil.sha256(rawRefreshToken);
        RefreshToken token = refreshTokenRepo.findByTokenHash(hash).orElse(null);

        if (token == null || token.isRevoked()) {
            return;
        }

        token.setRevoked(true);
        refreshTokenRepo.save(token);
        auditService.log(AuditAction.LOGOUT, token.getUser().getId(), ip, null);
    }
}