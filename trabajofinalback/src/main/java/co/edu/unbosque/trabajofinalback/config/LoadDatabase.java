package co.edu.unbosque.trabajofinalback.config;

import co.edu.unbosque.trabajofinalback.model.User;
import co.edu.unbosque.trabajofinalback.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Carga datos iniciales: un administrador y un usuario normal (con correo ya verificado) si no
 * existen. La contraseña inicial viene de la propiedad app.seed.password (variable SEED_PASSWORD) y
 * se guarda como hash BCrypt. En producción, desactivar con app.seed.enabled=false.
 */
@Configuration
public class LoadDatabase {

  private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

  @Bean
  @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
  CommandLineRunner initDatabase(
      UserRepository userRepo,
      PasswordEncoder passwordEncoder,
      @Value("${app.seed.password}") String seedPassword) {

    return args -> {
      if (userRepo.findByUsername("admin").isPresent()) {
        log.info("El administrador ya existe, omitiendo su creación...");
      } else {
        User admin =
            new User("admin", "admin@chat.local", passwordEncoder.encode(seedPassword), User.Role.ADMIN);
        admin.setEmailVerified(true);
        userRepo.save(admin);
        log.info("Precargando usuario administrador");
      }

      if (userRepo.findByUsername("normaluser").isPresent()) {
        log.info("El usuario normal ya existe, omitiendo su creación...");
      } else {
        User normal =
            new User("normaluser", "user@chat.local", passwordEncoder.encode(seedPassword), User.Role.USER);
        normal.setEmailVerified(true);
        userRepo.save(normal);
        log.info("Precargando usuario normal");
      }
    };
  }
}
