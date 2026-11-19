package co.edu.unbosque.trabajofinalback.dto;

/** Un elemento de la lista "data" de GIPHY. Solo se leen id, title e images. */
public class GiphyItemDTO {

	private String id;
	private String title;
	private GiphyImagesDTO images;

	public GiphyItemDTO() {
		// TODO Auto-generated constructor stub
	}

	public GiphyItemDTO(String id, String title, GiphyImagesDTO images) {
		this.id = id;
		this.title = title;
		this.images = images;
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

	public GiphyImagesDTO getImages() {
		return images;
	}

	public void setImages(GiphyImagesDTO images) {
		this.images = images;
	}

	@Override
	public String toString() {
		return "GiphyItemDTO [id=" + id + ", title=" + title + ", images=" + images + "]";
	}
}