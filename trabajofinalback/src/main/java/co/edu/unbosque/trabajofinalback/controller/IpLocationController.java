package co.edu.unbosque.trabajofinalback.controller;

import co.edu.unbosque.trabajofinalback.exception.IpApiException;
import co.edu.unbosque.trabajofinalback.service.IpLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta la ubicación de una IP. Solo para administradores (sirve para revisar la auditoría). */
@RestController
@RequestMapping("/ip")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Ubicación de IP", description = "Ubicación aproximada de una IP (ip-api.com)")
public class IpLocationController {

  @Autowired private IpLocationService ipServ;
  /** Crea el controlador que consulta la ubicación aproximada de una dirección IP. */
  public IpLocationController() {}

  /**
   * Consulta la ubicación aproximada de una dirección IP.
   *
   * @param ip dirección IPv4 o IPv6 que se desea ubicar
   * @return la ubicación aproximada o un mensaje de error si la IP es inválida o falla la consulta
   */
  @Operation(summary = "Ubicar una IP", description = "ip = dirección IPv4 o IPv6 a ubicar.")
  @GetMapping("/ubicacion")
  public ResponseEntity<?> ubicar(@RequestParam String ip) {
    if (ip == null || !ip.trim().matches("[0-9a-fA-F:.]{3,45}")) {
      return new ResponseEntity<>("La IP no es valida", HttpStatus.BAD_REQUEST);
    }
    try {
      return new ResponseEntity<>(ipServ.ubicar(ip.trim()), HttpStatus.OK);
    } catch (IpApiException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_GATEWAY);
    }
  }
}