package co.edu.unbosque.trabajofinalback.controller;

import co.edu.unbosque.trabajofinalback.model.Media;
import co.edu.unbosque.trabajofinalback.security.AuthContext;
import co.edu.unbosque.trabajofinalback.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Subida y descarga de imágenes y audios de los chats. Requiere token JWT. */
@RestController
@RequestMapping("/media")
@Transactional
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Multimedia", description = "Imágenes y audios de los chats, guardados cifrados")
public class MediaController {

  @Autowired private MediaService mediaServ;

  @Autowired private AuthContext authContext;

  public MediaController() {}

  /**
   * Envía una imagen o un audio a un chat y devuelve el resultado de la operación.
   */
  @Operation(
      summary = "Enviar imagen o audio a un chat",
      description =
          "Multipart con el campo 'file' y, opcional, 'contenido'. Imágenes (JPG, PNG, GIF, WEBP)"
              + " hasta 5 MB; audios (MP3, OGG, WEBM, WAV, M4A) hasta 10 MB")
  @PostMapping(path = "/upload/{chatId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<String> upload(
      @PathVariable Long chatId,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "contenido", required = false) String contenido,
      HttpServletRequest request) {
    int status =
        mediaServ.upload(authContext.getUserId(), chatId, file, contenido, request.getRemoteAddr());

    if (status == 0) {
      return new ResponseEntity<>("Archivo enviado exitosamente", HttpStatus.CREATED);
    } else if (status == 1) {
      return new ResponseEntity<>("El chat no existe", HttpStatus.NOT_FOUND);
    } else if (status == 2) {
      return new ResponseEntity<>("No participas en este chat", HttpStatus.FORBIDDEN);
    } else if (status == 3) {
      return new ResponseEntity<>("El archivo está vacío", HttpStatus.BAD_REQUEST);
    } else if (status == 4) {
      return new ResponseEntity<>(
          "Tipo de archivo no permitido. Solo imágenes (JPG, PNG, GIF, WEBP) y audios"
              + " (MP3, OGG, WEBM, WAV, M4A)",
          HttpStatus.BAD_REQUEST);
    } else {
      return new ResponseEntity<>(
          "Archivo demasiado grande. Imágenes hasta 5 MB y audios hasta 10 MB",
          HttpStatus.PAYLOAD_TOO_LARGE);
    }
  }

  /**
   * Descarga la imagen o el audio solicitado si el usuario tiene acceso.
   *
   * @param id identificador del archivo multimedia
   * @return el contenido descifrado o una respuesta 404 si no está disponible
   */
  @Operation(
      summary = "Descargar imagen o audio",
      description = "Solo para participantes del chat o administradores")
  @GetMapping("/getbyid/{id}")
  public ResponseEntity<byte[]> getById(@PathVariable Long id) {
    Media media = mediaServ.findAllowed(authContext.getUserId(), authContext.isAdmin(), id);
    if (media == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    byte[] datos = mediaServ.descifrar(media);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(media.getMimeType()));
    headers.setContentLength(datos.length);
    return new ResponseEntity<>(datos, headers, HttpStatus.OK);
  }
}