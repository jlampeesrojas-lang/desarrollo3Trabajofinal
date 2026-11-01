package co.edu.unbosque.trabajofinalback.controller;

import co.edu.unbosque.trabajofinalback.dto.AuthResponseDTO;
import co.edu.unbosque.trabajofinalback.dto.ConfirmEmailDTO;
import co.edu.unbosque.trabajofinalback.dto.LoginDTO;
import co.edu.unbosque.trabajofinalback.dto.MessageDTO;
import co.edu.unbosque.trabajofinalback.dto.RefreshTokenDTO;
import co.edu.unbosque.trabajofinalback.dto.RegisterDTO;
import co.edu.unbosque.trabajofinalback.dto.ResendConfirmationDTO;
import co.edu.unbosque.trabajofinalback.exception.PasswordNotValidException;
import co.edu.unbosque.trabajofinalback.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de autenticación: registro, confirmación de correo, login, refresh y logout.
 * Todos son POST con cuerpo JSON. Los errores salen como {"error": "...", "mensaje": "..."}.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Registro, confirmación de correo, login, refresh y logout")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /** IP de origen de la petición (para auditoría). */
  private String clientIp(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  @Operation(
      summary = "Registrar usuario",
      description =
          "Crea la cuenta con rol USER y envía un correo de confirmación. "
              + "Contraseña: mínimo 8 caracteres con minúscula, mayúscula, número y símbolo.")
  @PostMapping("/register")
  public ResponseEntity<MessageDTO> register(
      @Valid @RequestBody RegisterDTO dto, HttpServletRequest request)
      throws PasswordNotValidException {
    authService.register(dto, clientIp(request));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new MessageDTO("Usuario registrado. Revisa tu correo para confirmar la cuenta."));
  }

  @Operation(
      summary = "Confirmar correo",
      description = "Recibe el token del enlace del correo (un solo uso, vence en 24 horas).")
  @PostMapping("/confirm-email")
  public ResponseEntity<MessageDTO> confirmEmail(
      @Valid @RequestBody ConfirmEmailDTO dto, HttpServletRequest request) {
    authService.confirmEmail(dto.getToken(), clientIp(request));
    return ResponseEntity.ok(new MessageDTO("Correo confirmado. Ya puedes iniciar sesión."));
  }

  @Operation(
      summary = "Reenviar correo de confirmación",
      description = "Responde igual exista o no el correo, para no revelar qué correos están registrados.")
  @PostMapping("/resend-confirmation")
  public ResponseEntity<MessageDTO> resendConfirmation(
      @Valid @RequestBody ResendConfirmationDTO dto, HttpServletRequest request) {
    authService.resendConfirmation(dto.getEmail(), clientIp(request));
    return ResponseEntity.ok(
        new MessageDTO("Si el correo existe y está pendiente de confirmar, recibirás un mensaje."));
  }

  @Operation(
      summary = "Iniciar sesión",
      description =
          "Con correo o usuario más contraseña. Devuelve accessToken (JWT, 15 min) y refreshToken. "
              + "Enviar el access token en: Authorization: Bearer <token>.")
  @PostMapping("/login")
  public ResponseEntity<AuthResponseDTO> login(
      @Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
    return ResponseEntity.ok(authService.login(dto, clientIp(request)));
  }

  @Operation(
      summary = "Renovar sesión",
      description = "Cambia un refresh token por un access token nuevo y un refresh token nuevo (rotación).")
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponseDTO> refresh(
      @Valid @RequestBody RefreshTokenDTO dto, HttpServletRequest request) {
    return ResponseEntity.ok(authService.refresh(dto.getRefreshToken(), clientIp(request)));
  }

  @Operation(summary = "Cerrar sesión", description = "Revoca el refresh token enviado.")
  @PostMapping("/logout")
  public ResponseEntity<MessageDTO> logout(
      @Valid @RequestBody RefreshTokenDTO dto, HttpServletRequest request) {
    authService.logout(dto.getRefreshToken(), clientIp(request));
    return ResponseEntity.ok(new MessageDTO("Sesión cerrada"));
  }
}
