package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.Message;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de mensajes. */
public interface MessageRepository extends JpaRepository<Message, Long> {

  /** Historial paginado: lo más reciente primero. */
  @EntityGraph(attributePaths = "sender")
  Page<Message> findByChatIdOrderByIdDesc(Long chatId, Pageable pageable);

  /** Último mensaje del chat (para la lista de chats). */
  @EntityGraph(attributePaths = "sender")
  Optional<Message> findFirstByChatIdOrderByIdDesc(Long chatId);

  /** No leídos: mensajes de otros enviados después de la última lectura. */
  long countByChatIdAndSenderIdNotAndCreatedAtAfter(Long chatId, Long senderId, LocalDateTime after);

  /** No leídos cuando el usuario nunca ha abierto el chat. */
  long countByChatIdAndSenderIdNot(Long chatId, Long senderId);
}