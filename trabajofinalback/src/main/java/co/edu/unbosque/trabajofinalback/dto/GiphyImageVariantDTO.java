package co.edu.unbosque.trabajofinalback.dto;

/**
 * Una versión de la imagen de GIPHY: dirección y medidas (GIPHY las manda como
 * texto).
 */
public class GiphyImageVariantDTO {

	private String url;
	private String width;
	private String height;

	public GiphyImageVariantDTO() {
		// TODO Auto-generated constructor stub
	}

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