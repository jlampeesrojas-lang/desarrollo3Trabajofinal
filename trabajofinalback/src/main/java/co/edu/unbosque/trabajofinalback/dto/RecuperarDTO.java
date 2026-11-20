package co.edu.unbosque.trabajofinalback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Cuerpo de /auth/recuperar: el correo al que se enviará el código. */
public class RecuperarDTO {

  @NotBlank(message = "El correo es obligatorio")
  @Email(message = "El correo no tiene un formato válido")
  private String correo;

  /** Constructor vacío. */
  public RecuperarDTO() {}

  /** Crea un RecuperarDTO con sus datos. */
  public RecuperarDTO(String correo) {
    this.correo = correo;
  }

  /** Devuelve correo. */
  public String getCorreo() {
    return correo;
  }

  /** Define correo. */
  public void setCorreo(String correo) {
    this.correo = correo;
  }

  /** Devuelve el objeto como texto. */
  @Override
  public String toString() {
	return "RecuperarDTO [correo=" + correo + "]";
  }
  

}