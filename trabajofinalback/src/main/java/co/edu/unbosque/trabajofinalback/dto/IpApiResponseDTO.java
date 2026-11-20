package co.edu.unbosque.trabajofinalback.dto;

/**
 * Respuesta de ip-api.com. status es "success" o "fail"; si falla, message
 * explica el motivo (por ejemplo "private range"). Solo se leen los campos que
 * se piden en la URL.
 */
public class IpApiResponseDTO {
	private String status;
	private String message;
	private String country;
	private String regionName;
	private String city;
	private Double lat;
	private Double lon;
	private String query;

	/** Crea un DTO vacío para recibir la respuesta de la consulta de ubicación de una IP. */
	public IpApiResponseDTO() {
	}

	/** Devuelve status. */
	public String getStatus() {
		return status;
	}

	/** Define status. */
	public void setStatus(String status) {
		this.status = status;
	}

	/** Devuelve message. */
	public String getMessage() {
		return message;
	}

	/** Define message. */
	public void setMessage(String message) {
		this.message = message;
	}

	/** Devuelve country. */
	public String getCountry() {
		return country;
	}

	/** Define country. */
	public void setCountry(String country) {
		this.country = country;
	}

	/** Devuelve region name. */
	public String getRegionName() {
		return regionName;
	}

	/** Define region name. */
	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	/** Devuelve city. */
	public String getCity() {
		return city;
	}

	/** Define city. */
	public void setCity(String city) {
		this.city = city;
	}

	/** Devuelve lat. */
	public Double getLat() {
		return lat;
	}

	/** Define lat. */
	public void setLat(Double lat) {
		this.lat = lat;
	}

	/** Devuelve lon. */
	public Double getLon() {
		return lon;
	}

	/** Define lon. */
	public void setLon(Double lon) {
		this.lon = lon;
	}

	/** Devuelve query. */
	public String getQuery() {
		return query;
	}

	/** Define query. */
	public void setQuery(String query) {
		this.query = query;
	}

	/** Devuelve el objeto como texto. */
	@Override
	public String toString() {
		return "IpApiResponseDTO [status=" + status + ", country=" + country + ", regionName=" + regionName + ", city="
				+ city + ", lat=" + lat + ", lon=" + lon + ", query=" + query + "]";
	}
}