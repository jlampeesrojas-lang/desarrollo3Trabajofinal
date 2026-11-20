package co.edu.unbosque.trabajofinalback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de /auth/verificar-codigo: correo y código de 6 dígitos que llegó al
 * correo.
 */
public class VerificarCodigoDTO {

	@NotBlank(message = "El correo es obligatorio")
	@Email(message = "El correo no tiene un formato válido")
	private String correo;

	@NotBlank(message = "El código es obligatorio")
	@Pattern(regexp = "^\\d{6}$", message = "El código debe tener 6 dígitos")
	private String codigo;

	public VerificarCodigoDTO() {
		// TODO Auto-generated constructor stub
	}

	public VerificarCodigoDTO(String correo, String codigo) {
		this.correo = correo;
		this.codigo = codigo;
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
}
