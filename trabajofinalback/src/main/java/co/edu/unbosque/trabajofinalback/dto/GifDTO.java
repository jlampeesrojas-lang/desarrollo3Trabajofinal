package co.edu.unbosque.trabajofinalback.dto;

/**
 * GIF tal como lo devuelve este back al front (GET /gif/buscar).
 *
 * <p>
 * previewUrl es la versión liviana para mostrar la grilla del selector;
 * originalUrl es la versión completa que se manda en el chat. width y height
 * son las medidas de la vista previa.
 */
public class GifDTO {
	private String id;
	private String title;
	private String previewUrl;
	private String originalUrl;
	private String width;
	private String height;

	/** Crea un DTO vacío para los datos de un GIF. */
	public GifDTO() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Crea un DTO con los datos de un GIF.
	 *
	 * @param id identificador del GIF
	 * @param title título del GIF
	 * @param previewUrl URL de la vista previa
	 * @param originalUrl URL del GIF original
	 * @param width ancho del GIF en píxeles
	 * @param height alto del GIF en píxeles
	 */
	public GifDTO(String id, String title, String previewUrl, String originalUrl, String width, String height) {
		this.id = id;
		this.title = title;
		this.previewUrl = previewUrl;
		this.originalUrl = originalUrl;
		this.width = width;
		this.height = height;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getPreviewUrl() {
		return previewUrl;
	}

	public void setPreviewUrl(String previewUrl) {
		this.previewUrl = previewUrl;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public void setOriginalUrl(String originalUrl) {
		this.originalUrl = originalUrl;
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
		return "GifDTO [id=" + id + ", title=" + title + ", previewUrl=" + previewUrl + ", originalUrl=" + originalUrl
				+ ", width=" + width + ", height=" + height + "]";
	}
}