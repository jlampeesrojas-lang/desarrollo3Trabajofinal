package co.edu.unbosque.trabajofinalback.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilidad para tokens de un solo uso (correo y refresh). El token se genera con SecureRandom, se
 * entrega UNA vez al usuario y en la base de datos solo se guarda su hash SHA-256.
 */
public final class TokenUtil {

  private static final SecureRandom RANDOM = new SecureRandom();

  private TokenUtil() {}

  /**
   * Genera un token aleatorio de 256 bits, apto para URLs.
   *
   * @return Token en Base64 URL sin relleno (43 caracteres)
   */
  public static String generateToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /**
   * Calcula el hash SHA-256 de un token (reutiliza AESUtil, que usa commons-codec).
   *
   * @param token Token original
   * @return Hash en hexadecimal (64 caracteres)
   */
  public static String sha256(String token) {
    return AESUtil.hashingToSHA256(token);
  }
}
