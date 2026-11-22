package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.Message;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de mensajes e historial de conversaciones. */
public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Obtiene una página del historial con los mensajes más recientes primero. */
    @EntityGraph(attributePaths = "sender")
    Page<Message> findByChatIdOrderByIdDesc(Long chatId, Pageable pageable);

    /** Obtiene el último mensaje de la conversación. */
    @EntityGraph(attributePaths = "sender")
    Optional<Message> findFirstByChatIdOrderByIdDesc(Long chatId);

    /** Cuenta los mensajes guardados en el chat. */
    long countByChatId(Long chatId);

    /** Cuenta los mensajes posteriores a la fecha indicada cuyo remitente tiene otro ID. */
    long countByChatIdAndSenderIdNotAndCreatedAtAfter(
            Long chatId, Long senderId, LocalDateTime after);

    /** Cuenta los mensajes del chat cuyo remitente tiene un ID distinto al indicado. */
    long countByChatIdAndSenderIdNot(Long chatId, Long senderId);

    /** Obtiene los mensajes posteriores al último ID recibido, en orden de llegada. */
    @EntityGraph(attributePaths = "sender")
    List<Message> findByChatIdAndIdGreaterThanOrderByIdAsc(Long chatId, Long afterId);

    /** Obtiene los mensajes enviados por un usuario, del más reciente al más antiguo. */
    @EntityGraph(attributePaths = "sender")
    List<Message> findBySenderIdOrderByIdDesc(Long senderId, Pageable pageable);

    /** Lee un bloque de mensajes de los chats indicados entre dos identificadores. */
    @EntityGraph(attributePaths = "sender")
    List<Message> findByChatIdInAndIdGreaterThanAndIdLessThanOrderByIdDesc(
            List<Long> chats, Long despues, Long antes, Pageable pageable);
}