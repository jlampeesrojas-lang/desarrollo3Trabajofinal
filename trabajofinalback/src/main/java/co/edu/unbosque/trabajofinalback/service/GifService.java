package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.dto.GifDTO;
import co.edu.unbosque.trabajofinalback.exception.GifApiException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Busca GIFs en la API de GIPHY. La clave de la API nunca sale del back. */
@Service
public class GifService {

  /** Dirección de búsqueda de GIPHY (giphy.url en application.properties). */
  @Value("${giphy.url}")
  private String baseUrl;

  /** Clave de la API (variable de entorno GIPHY_API_KEY). */
  @Value("${giphy.api.key}")
  private String apiKey;

  public GifService() {}

  /**
   * Busca GIFs aptos para todo público.
   *
   * @param query texto a buscar
   * @param cantidad cuántos GIFs devolver
   * @return la lista de GIFs (vacía si no hay resultados)
   * @throws GifApiException si GIPHY falla o no hay clave configurada
   */
  public List<GifDTO> buscar(String query, int cantidad) throws GifApiException {
    if (apiKey == null || apiKey.isBlank()) {
      throw new GifApiException("Falta configurar la clave de la API de GIFs.");
    }

    String urlCompleta =
        baseUrl
            + "?api_key="
            + apiKey
            + "&q="
            + ExternalHTTPRequestHandler.codificar(query)
            + "&limit="
            + cantidad
            + "&rating=g&lang=es";

    return ExternalHTTPRequestHandler.consultarGifs(urlCompleta, cantidad);
  }
}