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
import java.time.LocalDateTime;
import java.util.Optional;
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

/**
 * Lógica de autenticación: registro con validación, confirmación de correo, login con JWT, refresh
 * con rotación y logout. Cada paso importante queda registrado en la auditoría.
 */
@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository userRepo;
  private final EmailTokenRepository emailTokenRepo;
  private final RefreshTokenRepository refreshTokenRepo;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;
  private final EmailService emailService;
  private final AuditService auditService;

  /** Horas de vida del enlace de confirmación. */
  @Value("${app.email-token-expiration-hours:24}")
  private long emailTokenHours;

  /** Días de vida del refresh token. */
  @Value("${jwt.refresh-expiration-days:7}")
  private long refreshDays;

  public AuthService(
      UserRepository userRepo,
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
   * Registra un usuario nuevo con rol USER, contraseña en hash BCrypt y correo sin confirmar, y le
   * envía el correo de confirmación.
   *
   * @param dto Datos ya validados por Bean Validation (@Valid)
   * @param ip IP de origen
   * @throws PasswordNotValidException si la contraseña no cumple la política
   * @throws UserAlreadyExistsException si el usuario o el correo ya existen
   */
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

    User user = new User(username, email, passwordEncoder.encode(dto.getPassword()), User.Role.USER);
    userRepo.save(user);
    auditService.log(AuditAction.REGISTER, user.getId(), ip, "email=" + email);

    sendConfirmation(user);
  }

  /** Crea un token de confirmación y lo envía. Si el SMTP falla, el usuario puede pedir reenvío. */
  private void sendConfirmation(User user) {
    String rawToken = TokenUtil.generateToken();
    emailTokenRepo.save(
        new EmailToken(
            user,
            TokenUtil.sha256(rawToken),
            TokenType.CONFIRM_EMAIL,
            LocalDateTime.now().plusHours(emailTokenHours)));
    try {
      emailService.sendConfirmationEmail(user, rawToken);
    } catch (MailException e) {
      log.error("No se pudo enviar el correo de confirmación a {}: {}", user.getEmail(), e.getMessage());
    }
  }

  // ------------------------------------------------------- CONFIRMACIÓN DE CORREO

  /**
   * Confirma el correo con el token recibido en el enlace. El token es de un solo uso.
   *
   * @param rawToken Token en texto plano
   * @param ip IP de origen
   * @throws InvalidTokenException (400) si no existe, venció o ya se usó
   */
  @Transactional
  public void confirmEmail(String rawToken, String ip) {
    EmailToken token =
        emailTokenRepo
            .findByTokenHash(TokenUtil.sha256(rawToken))
            .filter(t -> t.getType() == TokenType.CONFIRM_EMAIL)
            .orElseThrow(
                () -> new InvalidTokenException("El enlace de confirmación no es válido", HttpStatus.BAD_REQUEST));

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
   * Reenvía el correo de confirmación. Responde igual exista o no el correo, para no revelar qué
   * correos están registrados.
   */
  @Transactional
  public void resendConfirmation(String email, String ip) {
    Optional<User> found = userRepo.findByEmail(email.trim().toLowerCase());
    if (found.isPresent() && !found.get().isEmailVerified()) {
      User user = found.get();
      emailTokenRepo.invalidatePending(user, TokenType.CONFIRM_EMAIL, LocalDateTime.now());
      sendConfirmation(user);
      auditService.log(AuditAction.CONFIRMATION_RESENT, user.getId(), ip, null);
    }
  }

  // ------------------------------------------------------------------- LOGIN

  /**
   * Autentica con correo o usuario más contraseña y entrega access token (JWT) y refresh token.
   *
   * @param dto Credenciales
   * @param ip IP de origen
   * @return Tokens y datos de sesión
   * @throws AuthenticationException (401) credenciales incorrectas; (403) cuenta deshabilitada
   * @throws EmailNotVerifiedException (403) si aún no confirmó el correo
   */
  @Transactional
  public AuthResponseDTO login(LoginDTO dto, String ip) {
    String identifier = dto.getIdentifier().trim();
    Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(identifier, dto.getPassword()));
    } catch (AuthenticationException e) {
      Long userId =
          userRepo
              .findByUsername(identifier)
              .or(() -> userRepo.findByEmail(identifier.toLowerCase()))
              .map(User::getId)
              .orElse(null);
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

  /** Genera access token + un refresh token nuevo (se guarda solo su hash). */
  private AuthResponseDTO buildSession(User user) {
    String accessToken = jwtUtil.generateToken(user);
    String rawRefresh = TokenUtil.generateToken();
    refreshTokenRepo.save(
        new RefreshToken(
            user, TokenUtil.sha256(rawRefresh), LocalDateTime.now().plusDays(refreshDays)));
    return new AuthResponseDTO(
        accessToken,
        rawRefresh,
        jwtUtil.getAccessExpirationSeconds(),
        user.getUsername(),
        user.getRole().name());
  }

  // ----------------------------------------------------------------- REFRESH

  /**
   * Renueva la sesión con rotación: el refresh token usado se revoca y se entrega uno nuevo. Si
   * alguien presenta un refresh token ya revocado se asume robo y se cierran TODAS las sesiones del
   * usuario.
   *
   * @throws InvalidTokenException (401) si el token es inválido, está vencido o revocado
   */
  @Transactional(noRollbackFor = InvalidTokenException.class)
  public AuthResponseDTO refresh(String rawRefreshToken, String ip) {
    RefreshToken stored =
        refreshTokenRepo
            .findByTokenHash(TokenUtil.sha256(rawRefreshToken))
            .orElseThrow(
                () -> new InvalidTokenException("Refresh token inválido", HttpStatus.UNAUTHORIZED));

    User user = stored.getUser();

    if (stored.isRevoked()) {
      Long userId = user.getId(); // se lee antes de que revokeAllByUser limpie el contexto
      refreshTokenRepo.revokeAllByUser(user);
      auditService.log(AuditAction.TOKEN_REUSE_DETECTED, userId, ip, "refresh token reutilizado");
      throw new InvalidTokenException(
          "Sesión inválida, inicia sesión de nuevo", HttpStatus.UNAUTHORIZED);
    }
    if (stored.isExpired() || !user.isEnabled() || !user.isEmailVerified()) {
      throw new InvalidTokenException("La sesión venció, inicia sesión de nuevo", HttpStatus.UNAUTHORIZED);
    }

    stored.setRevoked(true);
    refreshTokenRepo.save(stored);
    auditService.log(AuditAction.TOKEN_REFRESH, user.getId(), ip, null);
    return buildSession(user);
  }

  // ------------------------------------------------------------------ LOGOUT

  /**
   * Cierra la sesión revocando el refresh token. Es idempotente: si el token no existe, no hace
   * nada.
   */
  @Transactional
  public void logout(String rawRefreshToken, String ip) {
    refreshTokenRepo
        .findByTokenHash(TokenUtil.sha256(rawRefreshToken))
        .filter(t -> !t.isRevoked())
        .ifPresent(
            t -> {
              t.setRevoked(true);
              refreshTokenRepo.save(t);
              auditService.log(AuditAction.LOGOUT, t.getUser().getId(), ip, null);
            });
  }
}
