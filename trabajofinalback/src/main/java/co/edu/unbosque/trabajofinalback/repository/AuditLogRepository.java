package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.model.AuditLog;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de los eventos de auditoría. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Obtiene una página de eventos del usuario indicado. */
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    /** Obtiene una página de eventos que registran la acción indicada. */
    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    /** Cuenta cuántas veces se registró una acción para un usuario. */
    long countByUserIdAndAction(Long userId, AuditAction action);

    /** Lee los eventos anteriores al identificador indicado, del más reciente al más antiguo. */
    List<AuditLog> findByIdLessThanOrderByIdDesc(Long id, Pageable pageable);
}