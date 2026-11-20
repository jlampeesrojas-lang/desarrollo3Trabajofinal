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

	/** Devuelve id. */
	public String getId() {
		return id;
	}

	/** Define id. */
	public void setId(String id) {
		this.id = id;
	}

	/** Devuelve title. */
	public String getTitle() {
		return title;
	}

	/** Define title. */
	public void setTitle(String title) {
		this.title = title;
	}

	/** Devuelve preview url. */
	public String getPreviewUrl() {
		return previewUrl;
	}

	/** Define preview url. */
	public void setPreviewUrl(String previewUrl) {
		this.previewUrl = previewUrl;
	}

	/** Devuelve original url. */
	public String getOriginalUrl() {
		return originalUrl;
	}

	/** Define original url. */
	public void setOriginalUrl(String originalUrl) {
		this.originalUrl = originalUrl;
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
		return "GifDTO [id=" + id + ", title=" + title + ", previewUrl=" + previewUrl + ", originalUrl=" + originalUrl
				+ ", width=" + width + ", height=" + height + "]";
	}
}