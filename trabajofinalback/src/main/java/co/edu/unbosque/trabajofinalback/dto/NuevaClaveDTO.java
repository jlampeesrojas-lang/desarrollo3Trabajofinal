package co.edu.unbosque.trabajofinalback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de /auth/nueva-clave: correo, código ya recibido y la contraseña
 * nueva.
 */
public class NuevaClaveDTO {

	@NotBlank(message = "El correo es obligatorio")
	@Email(message = "El correo no tiene un formato válido")
	private String correo;

	@NotBlank(message = "El código es obligatorio")
	@Pattern(regexp = "^\\d{6}$", message = "El código debe tener 6 dígitos")
	private String codigo;

	@NotBlank(message = "La contraseña es obligatoria")
	@Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
	private String clave;

	public NuevaClaveDTO() {
	}

	public NuevaClaveDTO(String correo, String codigo, String clave) {
		this.correo = correo;
		this.codigo = codigo;
		this.clave = clave;
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getClave() {
		return clave;
	}

	public void setClave(String clave) {
		this.clave = clave;
	}

}
