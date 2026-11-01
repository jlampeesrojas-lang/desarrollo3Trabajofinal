package co.edu.unbosque.trabajofinalback;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Clase principal de la aplicación Spring Boot (backend de la plataforma de mensajería). Sirve como
 * punto de entrada y configura los beans globales.
 *
 * @author Universidad El Bosque
 * @version 0.1
 */
@SpringBootApplication
public class TrabajofinalbackApplication {

  /**
   * Método principal que inicia la aplicación Spring Boot.
   *
   * @param args Argumentos de línea de comandos
   */
  public static void main(String[] args) {
    SpringApplication.run(TrabajofinalbackApplication.class, args);
  }

  /**
   * Bean de ModelMapper para mapear entre DTOs y entidades.
   *
   * @return Instancia de ModelMapper
   */
  @Bean
  public ModelMapper getModelMapper() {
    return new ModelMapper();
  }
}
