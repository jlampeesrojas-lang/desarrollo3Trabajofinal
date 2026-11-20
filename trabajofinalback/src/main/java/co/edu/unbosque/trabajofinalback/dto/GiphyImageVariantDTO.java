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

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}

	public String getHeight() {
		return height;
	}

	public void setHeight(String height) {
		this.height = height;
	}

	@Override
	public String toString() {
		return "GiphyImageVariantDTO [url=" + url + ", width=" + width + ", height=" + height + "]";
	}
}