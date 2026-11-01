package co.edu.unbosque.trabajofinalback.model;

/**
 * Tipos de eventos que se registran en la auditoría (tabla audit_log). Back 2 usa MESSAGE_SENT y
 * ADMIN_READ_CHAT; los demás los registra el módulo de seguridad.
 */
public enum AuditAction {
  REGISTER,
  EMAIL_CONFIRMED,
  CONFIRMATION_RESENT,
  LOGIN_OK,
  LOGIN_FAIL,
  LOGOUT,
  TOKEN_REFRESH,
  TOKEN_REUSE_DETECTED,
  PASSWORD_RESET_REQUESTED,
  PASSWORD_RESET_DONE,
  MESSAGE_SENT,
  ADMIN_READ_CHAT,
  ADMIN_USER_UPDATED
}
