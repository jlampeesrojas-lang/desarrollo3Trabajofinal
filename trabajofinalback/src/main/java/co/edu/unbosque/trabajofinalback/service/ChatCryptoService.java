package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.util.AESUtil;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;

/**
 * Cifrado de los mensajes usando el AESUtil del curso (AES/GCM).
 * Cada chat tiene su propia llave de 16 caracteres. Esa llave se guarda
 * en la tabla chats cifrada con la llave por defecto de AESUtil.
 */
@Service
public class ChatCryptoService {

  /** Vector de inicialización: el mismo de AESUtil (16 caracteres). */
  private static final String IV = "programacioncomp";

  private static final String LETRAS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  private final SecureRandom random = new SecureRandom();

  /** Genera una llave nueva de 16 caracteres para un chat. */
  public String generarLlave() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 16; i++) {
      sb.append(LETRAS.charAt(random.nextInt(LETRAS.length())));
    }
    return sb.toString();
  }

  /** Cifra la llave del chat antes de guardarla en la base de datos. */
  public String protegerLlave(String llave) {
    return AESUtil.encrypt(llave);
  }

  /** Recupera la llave del chat a partir de lo guardado en la base de datos. */
  public String recuperarLlave(String llaveGuardada) {
    return AESUtil.decrypt(llaveGuardada);
  }

  /** Cifra el texto de un mensaje con la llave del chat. */
  public String cifrar(String llave, String texto) {
    return AESUtil.encrypt(llave, IV, texto);
  }

  /** Descifra el texto de un mensaje con la llave del chat. */
  public String descifrar(String llave, String cifrado) {
    return AESUtil.decrypt(llave, IV, cifrado);
  
}
}