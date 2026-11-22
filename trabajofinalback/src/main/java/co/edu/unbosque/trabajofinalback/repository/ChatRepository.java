package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.Chat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de conversaciones. */
public interface ChatRepository extends JpaRepository<Chat, Long> {

    /** Comprueba si el código ya pertenece a una conversación. */
    boolean existsByCode(String code);

    /** Busca la conversación personal del usuario con la IA. */
    Optional<Chat> findByAiOwnerId(Long aiOwnerId);

    /** Lista las conversaciones por fecha del último mensaje para el panel de administración. */
    List<Chat> findAllByOrderByLastMessageAtDesc();
}