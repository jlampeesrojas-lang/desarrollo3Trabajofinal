package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.Message;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de mensajes e historial. */
public interface MessageRepository extends JpaRepository<Message, Long> {

	/** Obtiene una página del historial con los mensajes más recientes primero. */
	@EntityGraph(attributePaths = "sender")
	Page<Message> findByChatIdOrderByIdDesc(Long chatId, Pageable pageable);

	/** Obtiene el último mensaje de la conversación. */
	@EntityGraph(attributePaths = "sender")
	Optional<Message> findFirstByChatIdOrderByIdDesc(Long chatId);

	long countByChatId(Long chatId);

	long countByChatIdAndSenderIdNotAndCreatedAtAfter(Long chatId, Long senderId, LocalDateTime after);

	long countByChatIdAndSenderIdNot(Long chatId, Long senderId);

	/** Obtiene los mensajes nuevos en el orden en que llegaron. */
	@EntityGraph(attributePaths = "sender")
	List<Message> findByChatIdAndIdGreaterThanOrderByIdAsc(Long chatId, Long afterId);

	/** Obtiene los mensajes enviados por un usuario. */
	@EntityGraph(attributePaths = "sender")
	List<Message> findBySenderIdOrderByIdDesc(Long senderId, Pageable pageable);

	/** Lee un bloque de mensajes pertenecientes a los chats indicados. */
	@EntityGraph(attributePaths = "sender")
	List<Message> findByChatIdInAndIdGreaterThanAndIdLessThanOrderByIdDesc(List<Long> chats, Long despues, Long antes,
			Pageable pageable);
}