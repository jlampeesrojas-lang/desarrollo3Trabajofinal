package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.model.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio de la auditoría. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  /** Eventos de un usuario, paginados (para el panel de administración). */
  Page<AuditLog> findByUserId(Long userId, Pageable pageable);

  /** Eventos de un tipo, paginados. */
  Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

  /** Cuenta los eventos de un usuario y tipo. */
  long countByUserIdAndAction(Long userId, AuditAction action);

  /**
   * Búsqueda con filtros opcionales: si un filtro llega en null, no se aplica. Lo más reciente
   * primero.
   */
  @Query(
      "select a from AuditLog a "
          + "where (:userId is null or a.userId = :userId) "
          + "and (:action is null or a.action = :action) "
          + "and (:desde is null or a.createdAt >= :desde) "
          + "and (:hasta is null or a.createdAt < :hasta) "
          + "order by a.id desc")
  List<AuditLog> search(
      @Param("userId") Long userId,
      @Param("action") AuditAction action,
      @Param("desde") LocalDateTime desde,
      @Param("hasta") LocalDateTime hasta,
      Pageable pageable);
}