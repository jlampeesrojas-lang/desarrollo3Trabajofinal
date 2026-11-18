package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.ChatParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de participantes de chat. */
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

  /** Participaciones de un usuario, con el chat cargado. */
  @EntityGraph(attributePaths = "chat")
  List<ChatParticipant> findByUserId(Long userId);

  /** Participantes de un chat, con el usuario cargado. */
  @EntityGraph(attributePaths = "user")
  List<ChatParticipant> findByChatIdOrderByIdAsc(Long chatId);

  Optional<ChatParticipant> findByChatIdAndUserId(Long chatId, Long userId);

  boolean existsByChatIdAndUserId(Long chatId, Long userId);
}