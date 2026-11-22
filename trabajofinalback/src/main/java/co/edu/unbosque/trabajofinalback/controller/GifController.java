package co.edu.unbosque.trabajofinalback.controller;

import co.edu.unbosque.trabajofinalback.exception.GifApiException;
import co.edu.unbosque.trabajofinalback.service.GifService;
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

/**
 * Búsqueda de GIFs para el chat. Requiere token JWT. El front nunca llama a
 * GIPHY directamente.
 */
@RestController
@RequestMapping("/gif")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "GIFs", description = "Búsqueda de GIFs (GIPHY) para el chat")

/** Endpoints para buscar GIFs en Giphy. */
public class GifController {

	@Autowired
	private GifService gifServ;

	/** Crea el controlador de búsqueda de GIFs. */
	public GifController() {
	}

	/**
	 * Busca GIFs en GIPHY según el texto ingresado.
	 *
	 * @param query    texto que se desea buscar
	 * @param cantidad número máximo de GIFs que se devolverán, entre 1 y 30
	 * @return los GIFs encontrados o un mensaje de error si falla la búsqueda
	 */
	@Operation(summary = "Buscar GIFs", description = "query = texto a buscar, cantidad = cuántos GIFs devolver (1 a 30, por defecto 15).")
	@GetMapping("/buscar")
	public ResponseEntity<?> buscar(@RequestParam String query, @RequestParam(defaultValue = "15") int cantidad) {
		if (query == null || query.isBlank()) {
			return new ResponseEntity<>("Debes escribir que GIF quieres buscar", HttpStatus.BAD_REQUEST);
		}
		if (cantidad < 1 || cantidad > 30) {
			return new ResponseEntity<>("La cantidad debe estar entre 1 y 30", HttpStatus.BAD_REQUEST);
		}
		try {
			return new ResponseEntity<>(gifServ.buscar(query.trim(), cantidad), HttpStatus.OK);
		} catch (GifApiException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_GATEWAY);
		}
	}
}