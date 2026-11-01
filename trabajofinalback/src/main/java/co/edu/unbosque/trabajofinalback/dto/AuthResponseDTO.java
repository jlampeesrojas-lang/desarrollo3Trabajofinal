package co.edu.unbosque.trabajofinalback.dto;

/** Respuesta de login y refresh: access token (JWT), refresh token y datos básicos de sesión. */
public class AuthResponseDTO {

  private String accessToken;
  private String refreshToken;
  private String tokenType = "Bearer";
  private long expiresInSeconds;
  private String username;
  private String role;

  public AuthResponseDTO() {}

  public AuthResponseDTO(
      String accessToken, String refreshToken, long expiresInSeconds, String username, String role) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.expiresInSeconds = expiresInSeconds;
    this.username = username;
    this.role = role;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public String getTokenType() {
    return tokenType;
  }

  public long getExpiresInSeconds() {
    return expiresInSeconds;
  }

  public String getUsername() {
    return username;
  }

  public String getRole() {
    return role;
  }
}
