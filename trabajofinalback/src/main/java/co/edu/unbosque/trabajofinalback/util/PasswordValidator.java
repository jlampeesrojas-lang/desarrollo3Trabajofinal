package co.edu.unbosque.trabajofinalback.util;

import co.edu.unbosque.trabajofinalback.exception.PasswordNotValidException;
import java.nio.charset.StandardCharsets;

/**
 * Valida la política de contraseñas: mínimo 8 caracteres, una minúscula, una mayúscula, un número
 * y un símbolo. BCrypt solo procesa los primeros 72 bytes, por eso también se rechaza una
 * contraseña más larga.
 */
public final class PasswordValidator {

  private static final int MIN_LENGTH = 8;
  private static final int MAX_BYTES = 72;

  private PasswordValidator() {}

  /**
   * Valida una contraseña en texto plano.
   *
   * @param password Contraseña a validar
   * @throws PasswordNotValidException si no cumple la política
   */
  public static void validate(String password) throws PasswordNotValidException {
    if (password == null
        || password.length() < MIN_LENGTH
        || password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES
        || !password.matches(".*[a-z].*")
        || !password.matches(".*[A-Z].*")
        || !password.matches(".*\\d.*")
        || !password.matches(".*[^A-Za-z0-9].*")) {
      throw new PasswordNotValidException();
    }
  }
}
