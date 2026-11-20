package co.edu.unbosque.trabajofinalback.dto;

import java.util.List;
import java.util.ArrayList;

/**
 * Raíz de la respuesta de GIPHY al buscar. Solo se lee la lista "data"; meta y
 * pagination se ignoran.
 */
public class GiphySearchResponseDTO {

	private List<GiphyItemDTO> data;

	/** Crea un DTO vacío para recibir los resultados de una búsqueda en GIPHY. */
	public GiphySearchResponseDTO() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Crea una respuesta con los resultados de una búsqueda en GIPHY.
	 *
	 * @param data lista de GIFs encontrados
	 */
	public GiphySearchResponseDTO(List<GiphyItemDTO> data) {
	    if (data != null) {
	        this.data = new ArrayList<>(data);
	    }
	}

	public List<GiphyItemDTO> getData() {
		return data;
	}

	public void setData(List<GiphyItemDTO> data) {
	    if (data == null) {
	        this.data = null;
	    } else {
	        this.data = new ArrayList<>(data);
	    }
	}

	@Override
	public String toString() {
		return "GiphySearchResponseDTO [data=" + data + "]";
	}
}