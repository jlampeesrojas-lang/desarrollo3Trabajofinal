package co.edu.unbosque.trabajofinalback.dto;

/**
 * Una versión de la imagen de GIPHY: dirección y medidas (GIPHY las manda como
 * texto).
 */
public class GiphyImageVariantDTO {

	private String url;
	private String width;
	private String height;

	/** Crea un DTO vacío para la URL y las dimensiones de una variante del GIF. */
	public GiphyImageVariantDTO() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Crea un DTO con la URL y las dimensiones de una variante del GIF.
	 *
	 * @param url dirección de la variante del GIF
	 * @param width ancho de la variante en píxeles
	 * @param height alto de la variante en píxeles
	 */
	public GiphyImageVariantDTO(String url, String width, String height) {
		this.url = url;
		this.width = width;
		this.height = height;
	}

	/** Devuelve url. */
	public String getUrl() {
		return url;
	}

	/** Define url. */
	public void setUrl(String url) {
		this.url = url;
	}

	/** Devuelve width. */
	public String getWidth() {
		return width;
	}

	/** Define width. */
	public void setWidth(String width) {
		this.width = width;
	}

	/** Devuelve height. */
	public String getHeight() {
		return height;
	}

	/** Define height. */
	public void setHeight(String height) {
		this.height = height;
	}

	/** Devuelve el objeto como texto. */
	@Override
	public String toString() {
		return "GiphyImageVariantDTO [url=" + url + ", width=" + width + ", height=" + height + "]";
	}
}