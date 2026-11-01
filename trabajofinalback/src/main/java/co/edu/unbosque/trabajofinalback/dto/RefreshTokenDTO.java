package co.edu.unbosque.trabajofinalback.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de /auth/refresh y /auth/logout. */
public class RefreshTokenDTO {

  @NotBlank(message = "El refresh token es obligatorio")
  private String refreshToken;

  public RefreshTokenDTO() {}

  public RefreshTokenDTO(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
