package co.edu.unbosque.trabajofinalback.security;

import co.edu.unbosque.trabajofinalback.model.User.Role;

/**
 * Contrato para saber QUIÉN está haciendo la petición actual. Back 2 programa contra esta interfaz;
 * en desarrollo puede usar una implementación falsa con @Profile("dev") y en producción se usa
 * AuthContextImpl, que lee el JWT ya validado.
 */
public interface AuthContext {

  /** @return id del usuario autenticado (lanza IllegalStateException si no hay sesión) */
  Long getUserId();

  /** @return nombre de usuario del usuario autenticado */
  String getUsername();

  /** @return rol del usuario autenticado */
  Role getRole();

  /** @return true si el usuario autenticado es ADMIN */
  boolean isAdmin();
}
