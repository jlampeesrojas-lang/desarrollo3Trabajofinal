package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.Message;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio de mensajes. */
public interface MessageRepository extends JpaRepository<Message, Long> {

  /** Historial paginado: lo más reciente primero. */
  @EntityGraph(attributePaths = "sender")
  Page<Message> findByChatIdOrderByIdDesc(Long chatId, Pageable pageable);

  /** Último mensaje del chat. */
  @EntityGraph(attributePaths = "sender")
  Optional<Message> findFirstByChatIdOrderByIdDesc(Long chatId);

  /** Cantidad total de mensajes de un chat. */
  long countByChatId(Long chatId);

  /** No leídos: mensajes de otros enviados después de la última lectura. */
  long countByChatIdAndSenderIdNotAndCreatedAtAfter(
      Long chatId, Long senderId, LocalDateTime after);

  /** No leídos cuando el usuario nunca ha abierto el chat. */
  long countByChatIdAndSenderIdNot(Long chatId, Long senderId);

  /** Mensajes de un chat con id mayor a afterId, del más antiguo al más reciente. */
  @EntityGraph(attributePaths = "sender")
  List<Message> findByChatIdAndIdGreaterThanOrderByIdAsc(Long chatId, Long afterId);

  /** Mensajes enviados por un usuario en todos sus chats, lo más reciente primero. */
  @EntityGraph(attributePaths = "sender")
  List<Message> findBySenderIdOrderByIdDesc(Long senderId, Pageable pageable);

  /**
   * Mensajes recibidos por un usuario: los de sus chats que envió otra persona, con id mayor a
   * afterId. Lo más reciente primero.
   */
  @EntityGraph(attributePaths = "sender")
  @Query(
      "select m from Message m "
          + "where m.id > :afterId "
          + "and (m.sender is null or m.sender.id <> :userId) "
          + "and m.chat.id in (select p.chat.id from ChatParticipant p where p.user.id = :userId) "
          + "order by m.id desc")
  List<Message> findReceived(
      @Param("userId") Long userId, @Param("afterId") Long afterId, Pageable pageable);
}