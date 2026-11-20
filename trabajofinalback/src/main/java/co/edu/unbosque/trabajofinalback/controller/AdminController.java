package co.edu.unbosque.trabajofinalback.controller;

import co.edu.unbosque.trabajofinalback.dto.AuditLogDTO;
import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.dto.AuditAdminDTO;
import co.edu.unbosque.trabajofinalback.dto.ChatAdminDTO;
import co.edu.unbosque.trabajofinalback.dto.ClaveChatDTO;
import co.edu.unbosque.trabajofinalback.dto.MensajeAdminDTO;
import co.edu.unbosque.trabajofinalback.security.AuthContext;
import co.edu.unbosque.trabajofinalback.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del panel de administración. SecurityConfig ya exige rol ADMIN para todo /admin/**, así
 * que un USER recibe 403 y sin token recibe 401.
 */
@RestController
@RequestMapping("/admin")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administración", description = "Auditoría y lectura de chats (solo ADMIN)")
public class AdminController {

  private final AdminService adminService;
  private final AuthContext authContext;

  /** Crea el controlador con el servicio de administración y el contexto de autenticación. */
  public AdminController(AdminService adminService, AuthContext authContext) {
    this.adminService = adminService;
    this.authContext = authContext;
  }

  /** Devuelve los últimos eventos de auditoría, del más nuevo al más viejo. */
  @Operation(
      summary = "Auditoría",
      description = "Últimos eventos, del más nuevo al más viejo. Parámetro limit (1 a 500).")
  @GetMapping("/audit")
  public ResponseEntity<List<AuditAdminDTO>> audit(
      @RequestParam(name = "limit", defaultValue = "100") int limit) {
    return ResponseEntity.ok(adminService.audit(limit));
  }

  /** Devuelve la lista de chats con sus participantes y cantidad de mensajes. */
  @Operation(summary = "Lista de chats", description = "Código, participantes y cantidad de mensajes.")
  @GetMapping("/chats")
  public ResponseEntity<List<ChatAdminDTO>> chats() {
    return ResponseEntity.ok(adminService.chats());
  }

  /** Devuelve los mensajes descifrados de un chat si la clave enviada es la correcta. */
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

  /** Devuelve la clave de un chat y deja el acceso registrado en la auditoría. */
  @Operation(summary = "Clave de un chat", description = "Devuelve {\"key\": \"...\"} y queda auditado.")
  @GetMapping("/chats/{id}/key")
  public ResponseEntity<ClaveChatDTO> key(@PathVariable Long id, HttpServletRequest request) {
    return ResponseEntity.ok(
        new ClaveChatDTO(
            adminService.chatKey(id, authContext.getUserId(), request.getRemoteAddr())));
  }
}