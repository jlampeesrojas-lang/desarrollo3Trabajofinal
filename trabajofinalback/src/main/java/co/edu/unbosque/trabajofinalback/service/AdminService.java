package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.dto.AuditLogDTO;
import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.model.AuditLog;
import co.edu.unbosque.trabajofinalback.model.User;
import co.edu.unbosque.trabajofinalback.repository.AuditLogRepository;
import co.edu.unbosque.trabajofinalback.repository.RefreshTokenRepository;
import co.edu.unbosque.trabajofinalback.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** Operaciones del administrador: activar/desactivar usuarios y consultar la auditoría. */
@Service
public class AdminService {

  private static final int MAX_LIMIT = 500;

  @Autowired private UserRepository userRepo;

  @Autowired private RefreshTokenRepository refreshTokenRepo;

  @Autowired private AuditLogRepository auditRepo;

  @Autowired private AuditService auditService;

  public AdminService() {}

  /**
   * Desactiva un usuario: ya no puede iniciar sesión y se cierran todas sus sesiones.
   *
   * @return 0 = desactivado, 1 = no existe, 2 = no puedes desactivarte a ti mismo, 3 = ya estaba
   *     desactivado
   */
  public int disableUser(Long adminId, Long userId, String ip) {
    Optional<User> found = userRepo.findById(userId);
    if (!found.isPresent()) {
      return 1;
    }
    if (userId.equals(adminId)) {
      return 2;
    }
    User user = found.get();
    if (!user.isEnabled()) {
      return 3;
    }

    user.setEnabled(false);
    userRepo.save(user);
    // Cierra todas sus sesiones: sus refresh tokens dejan de servir
    refreshTokenRepo.revokeAllByUser(user);

    auditService.log(
        AuditAction.ADMIN_USER_UPDATED, adminId, ip, "desactivó al usuario id=" + userId);
    return 0;
  }

  /**
   * Vuelve a activar un usuario (y le quita un bloqueo por intentos fallidos si lo tenía).
   *
   * @return 0 = activado, 1 = no existe, 2 = ya estaba activo
   */
  public int enableUser(Long adminId, Long userId, String ip) {
    Optional<User> found = userRepo.findById(userId);
    if (!found.isPresent()) {
      return 1;
    }
    User user = found.get();
    if (user.isEnabled() && user.getLockedUntil() == null) {
      return 2;
    }

    user.setEnabled(true);
    user.setFailedAttempts(0);
    user.setLockedUntil(null);
    userRepo.save(user);

    auditService.log(
        AuditAction.ADMIN_USER_UPDATED, adminId, ip, "activó al usuario id=" + userId);
    return 0;
  }

  /**
   * Consulta la auditoría con filtros opcionales (null = sin filtro). Lo más reciente primero.
   *
   * @param username si se envía, se busca su id y se filtra por él
   * @param limit cuántos registros devolver (1 a 500; por defecto 50)
   */
  public List<AuditLogDTO> searchAudit(
      Long userId,
      String username,
      AuditAction action,
      LocalDateTime desde,
      LocalDateTime hasta,
      int limit) {
    if (limit < 1 || limit > MAX_LIMIT) {
      limit = 50;
    }
    if (username != null && !username.isBlank()) {
      Optional<User> u = userRepo.findByUsername(username.trim());
      if (!u.isPresent()) {
        return new ArrayList<>();
      }
      userId = u.get().getId();
    }

    List<AuditLog> registros =
        auditRepo.search(userId, action, desde, hasta, PageRequest.of(0, limit));

    // Nombres de usuario en una sola consulta (en vez de una por cada registro)
    Set<Long> ids = new HashSet<>();
    registros.forEach(
        (a) -> {
          if (a.getUserId() != null) {
            ids.add(a.getUserId());
          }
        });
    Map<Long, String> nombres = new HashMap<>();
    userRepo
        .findAllById(ids)
        .forEach(
            (u) -> {
              nombres.put(u.getId(), u.getUsername());
            });

    List<AuditLogDTO> dtoList = new ArrayList<>();
    registros.forEach(
        (a) -> {
          AuditLogDTO dto = new AuditLogDTO();
          dto.setId(a.getId());
          dto.setUserId(a.getUserId());
          dto.setUsername(a.getUserId() != null ? nombres.get(a.getUserId()) : null);
          dto.setAction(a.getAction().name());
          dto.setIp(a.getIp());
          dto.setLocation(a.getLocation());
          dto.setDetail(a.getDetail());
          dto.setCreatedAt(a.getCreatedAt().toString());
          dtoList.add(dto);
        });
    return dtoList;
  }
}