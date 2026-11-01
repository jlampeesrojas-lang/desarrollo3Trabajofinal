package co.edu.unbosque.trabajofinalback.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Pruebas unitarias del hash BCrypt: la contraseña nunca se guarda en texto plano. */
class PasswordHashTest {

  private final PasswordEncoder encoder = new BCryptPasswordEncoder(10);

  @Test
  void elHashNoEsLaContrasenaEnTextoPlano() {
    String hash = encoder.encode("Clave#12345");
    assertNotEquals("Clave#12345", hash);
    assertFalse(hash.contains("Clave#12345"));
    assertTrue(hash.startsWith("$2a$10$"), "Debe ser un hash BCrypt");
    assertEquals(60, hash.length());
  }

  @Test
  void matchesAceptaLaClaveCorrectaYRechazaLaIncorrecta() {
    String hash = encoder.encode("Clave#12345");
    assertTrue(encoder.matches("Clave#12345", hash));
    assertFalse(encoder.matches("clave#12345", hash));
    assertFalse(encoder.matches("Otra#12345", hash));
  }

  @Test
  void laMismaClaveGeneraHashesDistintosPorElSalt() {
    String hash1 = encoder.encode("Clave#12345");
    String hash2 = encoder.encode("Clave#12345");
    assertNotEquals(hash1, hash2);
    assertTrue(encoder.matches("Clave#12345", hash1));
    assertTrue(encoder.matches("Clave#12345", hash2));
  }
}
