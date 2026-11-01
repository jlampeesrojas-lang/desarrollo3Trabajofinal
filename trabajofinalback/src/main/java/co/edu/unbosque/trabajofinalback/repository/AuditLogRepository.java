package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de la auditoría. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  /** Eventos de un usuario, paginados (para el panel de administración). */
  Page<AuditLog> findByUserId(Long userId, Pageable pageable);

  /** Eventos de un tipo, paginados. */
  Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

  /** Cuenta los eventos de un usuario y tipo. */
  long countByUserIdAndAction(Long userId, AuditAction action);
}
