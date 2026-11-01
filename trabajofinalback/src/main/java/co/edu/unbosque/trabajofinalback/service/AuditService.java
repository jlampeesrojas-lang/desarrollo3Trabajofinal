package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.model.AuditAction;

/**
 * Contrato de auditoría. Cualquier módulo (por ejemplo el de chats de Back 2) registra actividad
 * llamando a log(...). Los metadatos NUNCA deben incluir contraseñas, tokens, claves ni el
 * contenido de los mensajes.
 */
public interface AuditService {

  /**
   * Registra un evento sin ubicación.
   *
   * @param action Tipo de evento
   * @param userId Id del usuario (null si es anónimo)
   * @param ip IP de origen
   * @param detail Metadatos del evento (máx. 500 caracteres)
   */
  void log(AuditAction action, Long userId, String ip, String detail);

  /**
   * Registra un evento con ubicación aproximada.
   *
   * @param action Tipo de evento
   * @param userId Id del usuario (null si es anónimo)
   * @param ip IP de origen
   * @param location Ciudad/país
   * @param detail Metadatos del evento (máx. 500 caracteres)
   */
  void log(AuditAction action, Long userId, String ip, String location, String detail);
}
