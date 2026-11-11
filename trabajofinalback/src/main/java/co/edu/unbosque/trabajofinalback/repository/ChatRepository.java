package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.Chat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio de chats. */
public interface ChatRepository extends JpaRepository<Chat, Long> {

  boolean existsByCode(String code);

  Optional<Chat> findByCode(String code);

  /** Chats 1 a 1 que comparten los dos usuarios. */
  @Query(
      "select p1.chat from ChatParticipant p1, ChatParticipant p2 "
          + "where p1.chat = p2.chat and p1.user.id = :a and p2.user.id = :b")
  List<Chat> findSharedChats(@Param("a") Long userA, @Param("b") Long userB);
}