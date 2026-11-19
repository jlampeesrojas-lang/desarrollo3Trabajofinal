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

	public IpLocationDTO() {
		// TODO Auto-generated constructor stub
	}

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

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return "IpLocationDTO [ip=" + ip + ", text=" + text + "]";
	}
}