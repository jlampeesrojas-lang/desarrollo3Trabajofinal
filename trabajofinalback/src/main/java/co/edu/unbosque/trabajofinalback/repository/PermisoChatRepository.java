package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.PermisoChat;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de permisos temporales de lectura de chats. */
public interface PermisoChatRepository extends JpaRepository<PermisoChat, String> {

    /** Comprueba si el permiso pertenece al usuario y al chat y sigue vigente. */
    boolean existsByCodigoAndUsuarioIdAndChatIdAndVenceEnAfter(
            String codigo, Long usuarioId, Long chatId, Instant ahora);

    /** Elimina el permiso de esa pestaña al bloquear el chat. */
    void deleteByCodigoAndUsuarioIdAndChatId(
            String codigo, Long usuarioId, Long chatId);
}