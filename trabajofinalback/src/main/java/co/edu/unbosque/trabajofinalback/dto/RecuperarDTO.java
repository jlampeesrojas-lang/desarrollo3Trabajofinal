package co.edu.unbosque.trabajofinalback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Cuerpo de /auth/recuperar: el correo al que se enviará el código. */
public class RecuperarDTO {

  @NotBlank(message = "El correo es obligatorio")
  @Email(message = "El correo no tiene un formato válido")
  private String correo;

  public RecuperarDTO() {}

  public RecuperarDTO(String correo) {
    this.correo = correo;
  }

  public String getCorreo() {
    return correo;
  }

  public void setCorreo(String correo) {
    this.correo = correo;
  }

  @Override
  public String toString() {
	return "RecuperarDTO [correo=" + correo + "]";
  }
  

}
