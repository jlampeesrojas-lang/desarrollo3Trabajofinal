package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.model.AuditLog;
import co.edu.unbosque.trabajofinalback.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de AuditService que guarda en MySQL (tabla audit_log). Usa una transacción propia
 * (REQUIRES_NEW) para que el registro se conserve aunque la operación que lo originó falle, por
 * ejemplo un login fallido.
 */
@Service
public class AuditServiceImpl implements AuditService {

  private static final int MAX_DETAIL = 500;

  private final AuditLogRepository auditLogRepository;

  public AuditServiceImpl(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void log(AuditAction action, Long userId, String ip, String detail) {
    log(action, userId, ip, null, detail);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void log(AuditAction action, Long userId, String ip, String location, String detail) {
    String safeDetail =
        detail != null && detail.length() > MAX_DETAIL ? detail.substring(0, MAX_DETAIL) : detail;
    auditLogRepository.save(new AuditLog(action, userId, ip, location, safeDetail));
  }
}
