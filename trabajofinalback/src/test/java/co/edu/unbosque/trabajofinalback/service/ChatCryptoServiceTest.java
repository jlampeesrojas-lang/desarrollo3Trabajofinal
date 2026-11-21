package co.edu.unbosque.trabajofinalback.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * Pruebas del cifrado de mensajes: lo que se guarda en la base de datos no es legible y solo se
 * recupera con la llave correcta.
 */
class ChatCryptoServiceTest {

  private final ChatCryptoService crypto = new ChatCryptoService();

  /** El texto cifrado no contiene el mensaje original. */
  @Test
  void elTextoCifradoNoEsLegible() {
    String llave = crypto.generarLlave();
    String mensaje = "Hola, este mensaje es secreto";

    String cifrado = crypto.cifrar(llave, mensaje);

    assertNotEquals(mensaje, cifrado);
    assertFalse(cifrado.contains("Hola"));
    assertFalse(cifrado.contains("secreto"));
  }

  /** Al descifrar con la misma llave se obtiene el mensaje original, con tildes y emojis. */
  @Test
  void alDescifrarSeObtieneElOriginal() {
    String llave = crypto.generarLlave();
    String mensaje = "Qu\u00e9 tal, \u00f1and\u00fa \uD83D\uDE00";

    String cifrado = crypto.cifrar(llave, mensaje);

    assertEquals(mensaje, crypto.descifrar(llave, cifrado));
  }

  /** El mismo mensaje cifrado en dos chats con llaves distintas da resultados distintos. */
  @Test
  void llavesDistintasProducenCifradosDistintos() {
    String mensaje = "Mensaje repetido";

    String cifrado1 = crypto.cifrar(crypto.generarLlave(), mensaje);
    String cifrado2 = crypto.cifrar(crypto.generarLlave(), mensaje);

    assertNotEquals(cifrado1, cifrado2);
  }

  /** Con una llave distinta no se puede recuperar el mensaje. */
  @Test
  void conOtraLlaveNoSePuedeDescifrar() {
    String mensaje = "Solo para el chat correcto";
    String cifrado = crypto.cifrar(crypto.generarLlave(), mensaje);

    String resultado = crypto.descifrar(crypto.generarLlave(), cifrado);

    assertNotEquals(mensaje, resultado);
  }

  /** La llave del chat se guarda cifrada y se puede recuperar. */
  @Test
  void laLlaveDelChatSeGuardaCifrada() {
    String llave = crypto.generarLlave();

    String guardada = crypto.protegerLlave(llave);

    assertNotEquals(llave, guardada);
    assertFalse(guardada.contains(llave));
    assertEquals(llave, crypto.recuperarLlave(guardada));
  }

  /** Cada chat recibe una llave distinta de 16 caracteres. */
  @Test
  void generaLlavesDistintasDe16Caracteres() {
    String llave1 = crypto.generarLlave();
    String llave2 = crypto.generarLlave();

    assertEquals(16, llave1.length());
    assertEquals(16, llave2.length());
    assertNotEquals(llave1, llave2);
  }
}