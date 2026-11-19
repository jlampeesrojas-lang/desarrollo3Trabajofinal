package co.edu.unbosque.trabajofinalback.dto;

import java.util.List;

/**
 * Raíz de la respuesta de GIPHY al buscar. Solo se lee la lista "data"; meta y
 * pagination se ignoran.
 */
public class GiphySearchResponseDTO {

	private List<GiphyItemDTO> data;

	public GiphySearchResponseDTO() {
		// TODO Auto-generated constructor stub
	}

	public GiphySearchResponseDTO(List<GiphyItemDTO> data) {
		this.data = data;
	}

	public List<GiphyItemDTO> getData() {
		return data;
	}

	public void setData(List<GiphyItemDTO> data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "GiphySearchResponseDTO [data=" + data + "]";
	}
}