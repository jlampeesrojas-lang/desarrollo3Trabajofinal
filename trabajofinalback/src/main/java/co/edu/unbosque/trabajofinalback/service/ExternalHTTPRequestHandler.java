package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.dto.GifDTO;
import co.edu.unbosque.trabajofinalback.dto.GiphyImagesDTO;
import co.edu.unbosque.trabajofinalback.dto.GiphyImageVariantDTO;
import co.edu.unbosque.trabajofinalback.dto.GiphyItemDTO;
import co.edu.unbosque.trabajofinalback.dto.GiphySearchResponseDTO;
import co.edu.unbosque.trabajofinalback.exception.GifApiException;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase para manejar peticiones HTTP a servicios externos. Proporciona funcionalidad para realizar
 * solicitudes HTTP a APIs externas.
 */
public class ExternalHTTPRequestHandler {

  /**
   * Cliente HTTP compartido para realizar peticiones. Configurado con HTTP/2 y un tiempo de espera
   * de conexión de 10 segundos.
   */
  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_2)
          .followRedirects(HttpClient.Redirect.NORMAL)
          .connectTimeout(Duration.ofSeconds(10))
          .build();

  /** Codifica un texto para poder ponerlo dentro de una URL (espacios, tildes, símbolos). */
  public static String codificar(String texto) {
    return URLEncoder.encode(texto.trim(), StandardCharsets.UTF_8).replace("+", "%20");
  }

  /**
   * Consulta la búsqueda de GIPHY y devuelve los GIFs ya convertidos al formato de este back.
   *
   * @param url dirección completa de GIPHY, con api_key, q y limit
   * @param cantidad máximo de GIFs a devolver
   * @throws GifApiException si GIPHY no responde, responde con error o devuelve algo inesperado
   */
  public static List<GifDTO> consultarGifs(String url, int cantidad) throws GifApiException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(15))
            .build();

    HttpResponse<String> response = null;
    try {
      response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      e.printStackTrace();
    }

    if (response == null || response.statusCode() != 200) {
      throw new GifApiException();
    }
    return convertirAGifs(response.body(), cantidad);
  }

  /**
   * Convierte el JSON de GIPHY en una lista de GifDTO. Se salta los elementos que no traen las
   * imágenes que se necesitan.
   *
   * @param json cuerpo de la respuesta de GIPHY
   * @param cantidad máximo de GIFs a devolver
   * @throws GifApiException si el JSON no tiene la forma esperada
   */
  public static List<GifDTO> convertirAGifs(String json, int cantidad) throws GifApiException {
    GiphySearchResponseDTO respuesta = null;
    try {
      respuesta = new Gson().fromJson(json, GiphySearchResponseDTO.class);
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (respuesta == null || respuesta.getData() == null) {
      throw new GifApiException("La API de GIFs devolvio una respuesta inesperada.");
    }

    List<GifDTO> lista = new ArrayList<>();
    for (GiphyItemDTO item : respuesta.getData()) {
      if (lista.size() >= cantidad) {
        break;
      }
      GiphyImagesDTO imagenes = item.getImages();
      if (imagenes == null || imagenes.getFixedHeight() == null || imagenes.getOriginal() == null) {
        continue;
      }
      GiphyImageVariantDTO vista = imagenes.getFixedHeight();
      GifDTO dto = new GifDTO();
      dto.setId(item.getId());
      dto.setTitle(item.getTitle());
      dto.setPreviewUrl(vista.getUrl());
      dto.setOriginalUrl(imagenes.getOriginal().getUrl());
      dto.setWidth(vista.getWidth());
      dto.setHeight(vista.getHeight());
      lista.add(dto);
    }
    return lista;
  }
}