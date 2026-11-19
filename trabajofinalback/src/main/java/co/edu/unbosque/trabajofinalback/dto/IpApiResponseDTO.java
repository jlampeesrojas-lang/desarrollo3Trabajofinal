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

	public IpApiResponseDTO() {
		// TODO Auto-generated constructor stub
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getRegionName() {
		return regionName;
	}

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public Double getLat() {
		return lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}

	public Double getLon() {
		return lon;
	}

	public void setLon(Double lon) {
		this.lon = lon;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	@Override
	public String toString() {
		return "IpApiResponseDTO [status=" + status + ", country=" + country + ", regionName=" + regionName + ", city="
				+ city + ", lat=" + lat + ", lon=" + lon + ", query=" + query + "]";
	}
}