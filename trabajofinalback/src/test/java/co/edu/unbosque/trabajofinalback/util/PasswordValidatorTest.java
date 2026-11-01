package co.edu.unbosque.trabajofinalback.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.edu.unbosque.trabajofinalback.exception.PasswordNotValidException;
import org.junit.jupiter.api.Test;

/** Pruebas de la política de contraseñas. */
class PasswordValidatorTest {

  @Test
  void aceptaContrasenaFuerte() {
    assertDoesNotThrow(() -> PasswordValidator.validate("Clave#12345"));
  }

  @Test
  void rechazaContrasenasDebiles() {
    assertThrows(PasswordNotValidException.class, () -> PasswordValidator.validate(null));
    assertThrows(PasswordNotValidException.class, () -> PasswordValidator.validate("Ab#1"));
    assertThrows(PasswordNotValidException.class, () -> PasswordValidator.validate("clave#12345"));
    assertThrows(PasswordNotValidException.class, () -> PasswordValidator.validate("CLAVE#12345"));
    assertThrows(PasswordNotValidException.class, () -> PasswordValidator.validate("Clave#abcde"));
    assertThrows(PasswordNotValidException.class, () -> PasswordValidator.validate("Clave123456"));
  }

  @Test
  void rechazaContrasenaMayorA72Bytes() {
    assertThrows(
        PasswordNotValidException.class, () -> PasswordValidator.validate("Aa#1" + "x".repeat(70)));
  }

  @Test
  void tokenUtilGeneraTokensDistintosYHashDeSesentaYCuatro() {
    String a = TokenUtil.generateToken();
    String b = TokenUtil.generateToken();
    org.junit.jupiter.api.Assertions.assertNotEquals(a, b);
    org.junit.jupiter.api.Assertions.assertEquals(64, TokenUtil.sha256(a).length());
  }
}
