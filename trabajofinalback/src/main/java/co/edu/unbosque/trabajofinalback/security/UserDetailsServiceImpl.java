package co.edu.unbosque.trabajofinalback.security;

import co.edu.unbosque.trabajofinalback.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carga el usuario desde la base de datos para Spring Security. Acepta como identificador tanto el
 * nombre de usuario como el correo, así el login puede hacerse con cualquiera de los dos.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  public UserDetailsServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
    return userRepository
        .findByUsername(identifier)
        .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
        .orElseThrow(
            () -> new UsernameNotFoundException("Usuario no encontrado: " + identifier));
  }
}
