package co.edu.unbosque.trabajofinalback.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Versiones de imagen que trae GIPHY. Solo se leen fixed_height (liviana) y
 * original (completa).
 */
public class GiphyImagesDTO {

	@SerializedName("fixed_height")
	private GiphyImageVariantDTO fixedHeight;
	private GiphyImageVariantDTO original;

	/** Crea un DTO vacío para las versiones original y de altura fija de un GIF. */
	public GiphyImagesDTO() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Crea un DTO con las dos versiones del GIF.
	 *
	 * @param fixedHeight versión del GIF con altura fija
	 * @param original versión del GIF con sus dimensiones originales
	 */
	public GiphyImagesDTO(GiphyImageVariantDTO fixedHeight, GiphyImageVariantDTO original) {
		this.fixedHeight = fixedHeight;
		this.original = original;
	}

	public GiphyImageVariantDTO getFixedHeight() {
		return fixedHeight;
	}

	public void setFixedHeight(GiphyImageVariantDTO fixedHeight) {
		this.fixedHeight = fixedHeight;
	}

	public GiphyImageVariantDTO getOriginal() {
		return original;
	}

	public void setOriginal(GiphyImageVariantDTO original) {
		this.original = original;
	}

	@Override
	public String toString() {
		return "GiphyImagesDTO [fixedHeight=" + fixedHeight + ", original=" + original + "]";
	}
}