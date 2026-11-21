package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.model.AuditLog;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de los eventos de auditoría. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    long countByUserIdAndAction(Long userId, AuditAction action);

    /** Lee el siguiente bloque de eventos, desde el más reciente. */
    List<AuditLog> findByIdLessThanOrderByIdDesc(
            Long id, Pageable pageable);
}