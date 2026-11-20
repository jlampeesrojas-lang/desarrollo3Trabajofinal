package co.edu.unbosque.trabajofinalback.dto;

/**
 * Ubicación aproximada de una IP, tal como la devuelve este back (GET
 * /ip/ubicacion).
 *
 * <p>
 * text es el texto corto que se guarda en la auditoría, por ejemplo "Bogotá,
 * Colombia".
 */
public class IpLocationDTO {
	private String ip;
	private String country;
	private String region;
	private String city;
	private Double latitude;
	private Double longitude;
	private String text;

	/** Crea un DTO vacío para los datos de ubicación de una dirección IP. */
	public IpLocationDTO() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Crea un DTO con la ubicación aproximada de una dirección IP.
	 *
	 * @param ip dirección IP consultada
	 * @param country país asociado a la IP
	 * @param region región asociada a la IP
	 * @param city ciudad asociada a la IP
	 * @param latitude latitud aproximada en grados
	 * @param longitude longitud aproximada en grados
	 * @param text descripción de la ubicación que se muestra al usuario
	 */
	public IpLocationDTO(String ip, String country, String region, String city, Double latitude, Double longitude,
			String text) {
		this.ip = ip;
		this.country = country;
		this.region = region;
		this.city = city;
		this.latitude = latitude;
		this.longitude = longitude;
		this.text = text;
	}

	/** Devuelve ip. */
	public String getIp() {
		return ip;
	}

	/** Define ip. */
	public void setIp(String ip) {
		this.ip = ip;
	}

	/** Devuelve country. */
	public String getCountry() {
		return country;
	}

	/** Define country. */
	public void setCountry(String country) {
		this.country = country;
	}

	/** Devuelve region. */
	public String getRegion() {
		return region;
	}

	/** Define region. */
	public void setRegion(String region) {
		this.region = region;
	}

	/** Devuelve city. */
	public String getCity() {
		return city;
	}

	/** Define city. */
	public void setCity(String city) {
		this.city = city;
	}

	/** Devuelve latitude. */
	public Double getLatitude() {
		return latitude;
	}

	/** Define latitude. */
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	/** Devuelve longitude. */
	public Double getLongitude() {
		return longitude;
	}

	/** Define longitude. */
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	/** Devuelve text. */
	public String getText() {
		return text;
	}

	/** Define text. */
	public void setText(String text) {
		this.text = text;
	}

	/** Devuelve el objeto como texto. */
	@Override
	public String toString() {
		return "IpLocationDTO [ip=" + ip + ", text=" + text + "]";
	}
}