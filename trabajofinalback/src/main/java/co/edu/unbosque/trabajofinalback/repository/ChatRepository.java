package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.Chat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de conversaciones. */
public interface ChatRepository extends JpaRepository<Chat, Long> {

    boolean existsByCode(String code);

    /** Busca la conversación personal de IA del usuario. */
    Optional<Chat> findByAiOwnerId(Long aiOwnerId);

    /** Lista las conversaciones por actividad para el panel de administración. */
    List<Chat> findAllByOrderByLastMessageAtDesc();
}