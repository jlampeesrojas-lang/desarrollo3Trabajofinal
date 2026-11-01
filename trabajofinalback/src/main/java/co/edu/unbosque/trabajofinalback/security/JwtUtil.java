package co.edu.unbosque.trabajofinalback.security;

import co.edu.unbosque.trabajofinalback.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Utilidad para generar y validar JSON Web Tokens (access tokens). El secreto de firma viene de la
 * variable de entorno JWT_SECRET (mínimo 32 caracteres) y nunca se guarda en el repositorio.
 */
@Component
public class JwtUtil {

  /** Secreto de firma HS256 (propiedad jwt.secret, tomada de JWT_SECRET). */
  @Value("${jwt.secret}")
  private String secret;

  /** Duración del access token en milisegundos (por defecto 15 minutos). */
  @Value("${jwt.access-expiration-ms:900000}")
  private long accessExpirationMs;

  private Key getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /** @return Duración del access token en segundos (se le informa al cliente). */
  public long getAccessExpirationSeconds() {
    return accessExpirationMs / 1000;
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public String extractRole(String token) {
    return extractClaim(token, claims -> claims.get("role", String.class));
  }

  /**
   * Extrae un dato del token. Lanza JwtException (por ejemplo ExpiredJwtException) si la firma es
   * inválida o el token está vencido.
   */
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    return claimsResolver.apply(extractAllClaims(token));
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getSigningKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  /**
   * Genera un access token con el usuario (subject), su rol y su id.
   *
   * @param userDetails Usuario autenticado
   * @return JWT firmado
   */
  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    if (userDetails instanceof User user) {
      claims.put("role", user.getRole().name());
      claims.put("uid", user.getId());
    }
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(userDetails.getUsername())
        .setIssuedAt(new Date(now))
        .setExpiration(new Date(now + accessExpirationMs))
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * Valida que el token pertenezca al usuario y no esté vencido.
   *
   * @param token JWT recibido
   * @param userDetails Usuario cargado desde la base de datos
   * @return true si es válido
   */
  public boolean validateToken(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return username.equals(userDetails.getUsername())
        && !extractExpiration(token).before(new Date());
  }
}
