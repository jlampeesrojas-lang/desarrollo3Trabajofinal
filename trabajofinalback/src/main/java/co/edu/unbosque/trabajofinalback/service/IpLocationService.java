package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.dto.IpLocationDTO;
import co.edu.unbosque.trabajofinalback.exception.IpApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Ubica una IP con ip-api.com (sin clave). Se usa para llenar la ubicación de la auditoría. */
@Service
public class IpLocationService {

  /** Dirección base de ip-api.com (ipapi.url en application.properties). */
  @Value("${ipapi.url}")
  private String baseUrl;

  public IpLocationService() {}

  /**
   * Ubica una IP. Las IP de la red local no se consultan: se devuelven como "Red local".
   *
   * @param ip dirección IP a ubicar
   * @throws IpApiException si la API falla o rechaza la IP
   */
  public IpLocationDTO ubicar(String ip) throws IpApiException {
    if (esIpLocal(ip)) {
      return new IpLocationDTO(ip, null, null, null, null, null, "Red local");
    }

    String urlCompleta =
        baseUrl
            + ExternalHTTPRequestHandler.codificar(ip)
            + "?fields=status,message,country,regionName,city,lat,lon,query&lang=es";

    return ExternalHTTPRequestHandler.consultarUbicacionIp(urlCompleta);
  }

  /**
   * Devuelve el texto de ubicación para guardar en la auditoría. Nunca lanza error: si no se puede
   * ubicar, devuelve null y la auditoría se guarda igual, sin ubicación.
   *
   * @param ip dirección IP de origen del evento
   */
  public String textoUbicacion(String ip) {
    if (ip == null || ip.isBlank()) {
      return null;
    }
    try {
      return ubicar(ip).getText();
    } catch (IpApiException e) {
      return null;
    }
  }

  /** Dice si la IP es de la propia máquina o de una red privada (no se puede ubicar). */
  private boolean esIpLocal(String ip) {
    return ip.equals("127.0.0.1")
        || ip.equals("0:0:0:0:0:0:0:1")
        || ip.equals("::1")
        || ip.startsWith("10.")
        || ip.startsWith("192.168.")
        || ip.startsWith("172.16.");
  }
}