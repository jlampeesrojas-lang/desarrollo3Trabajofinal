package co.edu.unbosque.trabajofinalback.controller;

import co.edu.unbosque.trabajofinalback.dto.AuditLogDTO;
import co.edu.unbosque.trabajofinalback.model.AuditAction;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints del administrador. Solo rol ADMIN (regla /admin/** en SecurityConfig). */
@RestController
@RequestMapping("/admin")
@Transactional
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administración", description = "Gestión de usuarios y consulta de auditoría")
public class AdminController {

  @Autowired private AdminService adminServ;

  @Autowired private AuthContext authContext;

  public AdminController() {}

  @Operation(
      summary = "Desactivar usuario",
      description = "No podrá iniciar sesión y se cierran todas sus sesiones")
  @PutMapping("/disableuser/{id}")
  public ResponseEntity<String> disableUser(@PathVariable Long id, HttpServletRequest request) {
    int status = adminServ.disableUser(authContext.getUserId(), id, request.getRemoteAddr());

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

  @Operation(
      summary = "Activar usuario",
      description = "Lo vuelve a habilitar y le quita el bloqueo por intentos fallidos")
  @PutMapping("/enableuser/{id}")
  public ResponseEntity<String> enableUser(@PathVariable Long id, HttpServletRequest request) {
    int status = adminServ.enableUser(authContext.getUserId(), id, request.getRemoteAddr());

    if (status == 0) {
      return new ResponseEntity<>("Usuario activado exitosamente", HttpStatus.OK);
    } else if (status == 1) {
      return new ResponseEntity<>("El usuario no existe", HttpStatus.NOT_FOUND);
    } else {
      return new ResponseEntity<>("El usuario ya estaba activo", HttpStatus.NOT_ACCEPTABLE);
    }
  }

  @Operation(
      summary = "Consultar auditoría con filtros",
      description =
          "Todos los filtros son opcionales: limit (por defecto 50, máx. 500), userId, username,"
              + " action (ej. LOGIN_OK, LOGIN_FAIL, MESSAGE_SENT), desde y hasta (yyyy-MM-dd)")
  @GetMapping("/audit")
  public ResponseEntity<List<AuditLogDTO>> getAudit(
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String desde,
      @RequestParam(required = false) String hasta) {

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
        // "hasta" incluye todo ese día: se busca hasta el inicio del día siguiente
        fin = LocalDate.parse(hasta.trim()).plusDays(1).atStartOfDay();
      }
    } catch (DateTimeParseException e) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    List<AuditLogDTO> registros =
        adminServ.searchAudit(userId, username, accion, inicio, fin, limit);
    return new ResponseEntity<>(registros, HttpStatus.OK);
  }
}