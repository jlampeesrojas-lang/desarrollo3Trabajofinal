package co.edu.unbosque.trabajofinalback.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.unbosque.trabajofinalback.model.User;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Pruebas unitarias de JwtUtil: generación, lectura y token vencido rechazado. */
class JwtUtilTest {

  private static final String SECRET = "clave-de-pruebas-de-al-menos-32-caracteres-123456";

  private JwtUtil jwtUtil;
  private User user;

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil();
    ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    ReflectionTestUtils.setField(jwtUtil, "accessExpirationMs", 900_000L);
    user = new User("maria", "maria@correo.com", "hash", User.Role.ADMIN);
  }

  @Test
  void generaTokenConUsuarioYRol() {
    String token = jwtUtil.generateToken(user);
    assertEquals("maria", jwtUtil.extractUsername(token));
    assertEquals("ADMIN", jwtUtil.extractRole(token));
    assertTrue(jwtUtil.validateToken(token, user));
    assertEquals(900, jwtUtil.getAccessExpirationSeconds());
  }

  @Test
  void tokenDeOtroUsuarioNoEsValido() {
    String token = jwtUtil.generateToken(user);
    User otro = new User("pedro", "pedro@correo.com", "hash");
    assertFalse(jwtUtil.validateToken(token, otro));
  }

  @Test
  void tokenVencidoSeRechaza() {
    ReflectionTestUtils.setField(jwtUtil, "accessExpirationMs", -1_000L);
    String vencido = jwtUtil.generateToken(user);
    assertThrows(ExpiredJwtException.class, () -> jwtUtil.extractUsername(vencido));
  }

  @Test
  void tokenManipuladoSeRechaza() {
    String token = jwtUtil.generateToken(user);
    String manipulado = token.substring(0, token.length() - 3) + "abc";
    assertThrows(io.jsonwebtoken.JwtException.class, () -> jwtUtil.extractUsername(manipulado));
  }
}
