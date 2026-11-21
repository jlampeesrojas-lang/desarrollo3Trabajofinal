package co.edu.unbosque.trabajofinalback.controller;

import co.edu.unbosque.trabajofinalback.dto.AuditAdminDTO;
import co.edu.unbosque.trabajofinalback.dto.ChatAdminDTO;
import co.edu.unbosque.trabajofinalback.dto.ClaveChatDTO;
import co.edu.unbosque.trabajofinalback.dto.MensajeAdminDTO;
import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.security.AuthContext;
import co.edu.unbosque.trabajofinalback.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del panel de administración. SecurityConfig exige rol ADMIN para todo /admin/**, así
 * que un USER recibe 403 y sin token recibe 401.
 */
@RestController
@RequestMapping("/admin")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administración", description = "Auditoría, usuarios y lectura de chats (solo ADMIN)")
public class AdminController {

  private final AdminService adminService;
  private final AuthContext authContext;

  /**
   * Crea el controlador con el servicio de administración y el contexto de autenticación.
   *
   * @param adminService servicio de administración
   * @param authContext contexto con el usuario que hace la petición
   */
  public AdminController(AdminService adminService, AuthContext authContext) {
    this.adminService = adminService;
    this.authContext = authContext;
  }


  /**
   * Devuelve eventos de auditoría, del más reciente al más antiguo, con filtros opcionales.
   *
   * @param limit cantidad de eventos (1 a 500)
   * @param userId id del usuario
   * @param username nombre del usuario
   * @param action tipo de evento, por ejemplo LOGIN_OK, LOGIN_FAIL o MESSAGE_SENT
   * @param desde fecha inicial en formato yyyy-MM-dd
   * @param hasta fecha final en formato yyyy-MM-dd (incluye todo ese día)
   * @return eventos de auditoría, o 400 si la acción o una fecha no son válidas
   */
  @Operation(
      summary = "Auditoría con filtros",
      description =
          "Todos los filtros son opcionales: limit (1 a 500), userId, username, action"
              + " (ej. LOGIN_OK, LOGIN_FAIL, MESSAGE_SENT), desde y hasta (yyyy-MM-dd).")
  @GetMapping("/audit")
  public ResponseEntity<List<AuditAdminDTO>> audit(
      @RequestParam(name = "limit", defaultValue = "100") int limit,
      @RequestParam(name = "userId", required = false) Long userId,
      @RequestParam(name = "username", required = false) String username,
      @RequestParam(name = "action", required = false) String action,
      @RequestParam(name = "desde", required = false) String desde,
      @RequestParam(name = "hasta", required = false) String hasta) {

    AuditAction accion = null;
    if (action != null && !action.isBlank()) {
      try {
        accion = AuditAction.valueOf(action.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
      }
    }

    LocalDateTime inicio = null;
    LocalDateTime fin = null;
    try {
      if (desde != null && !desde.isBlank()) {
        inicio = LocalDate.parse(desde.trim()).atStartOfDay();
      }
      if (hasta != null && !hasta.isBlank()) {
        fin = LocalDate.parse(hasta.trim()).plusDays(1).atStartOfDay();
      }
    } catch (DateTimeParseException e) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    return ResponseEntity.ok(adminService.audit(limit, userId, username, accion, inicio, fin));
  }


  /**
   * Desactiva un usuario: no podrá iniciar sesión y se cierran todas sus sesiones.
   *
   * @param id id del usuario
   * @param request petición HTTP (para la IP)
   * @return mensaje con el resultado
   */
  @Operation(
      summary = "Desactivar usuario",
      description = "No podrá iniciar sesión y se cierran todas sus sesiones.")
  @PutMapping("/disableuser/{id}")
  public ResponseEntity<String> disableUser(@PathVariable Long id, HttpServletRequest request) {
    int status = adminService.disableUser(authContext.getUserId(), id, request.getRemoteAddr());

    if (status == 0) {
      return new ResponseEntity<>("Usuario desactivado exitosamente", HttpStatus.OK);
    } else if (status == 1) {
      return new ResponseEntity<>("El usuario no existe", HttpStatus.NOT_FOUND);
    } else if (status == 2) {
      return new ResponseEntity<>("No puedes desactivarte a ti mismo", HttpStatus.BAD_REQUEST);
    } else {
      return new ResponseEntity<>("El usuario ya estaba desactivado", HttpStatus.NOT_ACCEPTABLE);
    }
  }

  /**
   * Activa un usuario y le quita el bloqueo por intentos fallidos.
   *
   * @param id id del usuario
   * @param request petición HTTP (para la IP)
   * @return mensaje con el resultado
   */
  @Operation(
      summary = "Activar usuario",
      description = "Lo vuelve a habilitar y le quita el bloqueo por intentos fallidos.")
  @PutMapping("/enableuser/{id}")
  public ResponseEntity<String> enableUser(@PathVariable Long id, HttpServletRequest request) {
    int status = adminService.enableUser(authContext.getUserId(), id, request.getRemoteAddr());

    if (status == 0) {
      return new ResponseEntity<>("Usuario activado exitosamente", HttpStatus.OK);
    } else if (status == 1) {
      return new ResponseEntity<>("El usuario no existe", HttpStatus.NOT_FOUND);
    } else {
      return new ResponseEntity<>("El usuario ya estaba activo", HttpStatus.NOT_ACCEPTABLE);
    }
  }


  /**
   * Devuelve la lista de chats con sus participantes y cantidad de mensajes.
   *
   * @return chats del sistema
   */
  @Operation(summary = "Lista de chats", description = "Código, participantes y cantidad de mensajes.")
  @GetMapping("/chats")
  public ResponseEntity<List<ChatAdminDTO>> chats() {
    return ResponseEntity.ok(adminService.chats());
  }

  /**
   * Devuelve los mensajes descifrados de un chat si la clave enviada es la correcta.
   *
   * @param id id del chat
   * @param dto clave del chat
   * @param request petición HTTP (para la IP)
   * @return mensajes descifrados, o 400 si la clave es incorrecta
   */
  @Operation(
      summary = "Mensajes de un chat",
      description =
          "Body: {\"key\": \"clave del chat\"}. Si la clave es correcta devuelve los mensajes "
              + "descifrados; si no, 400.")
  @PostMapping("/chats/{id}/messages")
  public ResponseEntity<List<MensajeAdminDTO>> messages(
      @PathVariable Long id, @Valid @RequestBody ClaveChatDTO dto, HttpServletRequest request) {
    return ResponseEntity.ok(
        adminService.messages(id, dto.getKey(), authContext.getUserId(), request.getRemoteAddr()));
  }

  /**
   * Devuelve la clave de un chat y deja el acceso registrado en la auditoría.
   *
   * @param id id del chat
   * @param request petición HTTP (para la IP)
   * @return la clave del chat
   */
  @Operation(summary = "Clave de un chat", description = "Devuelve {\"key\": \"...\"} y queda auditado.")
  @GetMapping("/chats/{id}/key")
  public ResponseEntity<ClaveChatDTO> key(@PathVariable Long id, HttpServletRequest request) {
    return ResponseEntity.ok(
        new ClaveChatDTO(
            adminService.chatKey(id, authContext.getUserId(), request.getRemoteAddr())));
  }
}