package co.edu.unbosque.trabajofinalback.security;

import co.edu.unbosque.trabajofinalback.model.User;
import co.edu.unbosque.trabajofinalback.model.User.Role;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Implementación real de AuthContext: toma al usuario del SecurityContext (JWT validado). */
@Component
@Profile("!dev")
public class AuthContextImpl implements AuthContext {

  private User currentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
      return user;
    }
    throw new IllegalStateException("No hay un usuario autenticado en esta petición");
  }

  @Override
  public Long getUserId() {
    return currentUser().getId();
  }

  @Override
  public String getUsername() {
    return currentUser().getUsername();
  }

  @Override
  public Role getRole() {
    return currentUser().getRole();
  }

  @Override
  public boolean isAdmin() {
    return getRole() == Role.ADMIN;
  }
}
