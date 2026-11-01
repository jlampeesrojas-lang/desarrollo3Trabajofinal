package co.edu.unbosque.trabajofinalback.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuración de seguridad: sistema CERRADO. Solo /auth/** y la documentación Swagger son
 * públicos; cualquier otra ruta exige un JWT válido (401 si falta, 403 si el rol no alcanza).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthFilter;
  private final UserDetailsService userDetailsService;

  /** Orígenes permitidos para CORS, separados por coma (variable CORS_ORIGINS). */
  @Value("${app.cors.allowed-origins}")
  private String allowedOrigins;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthFilter, UserDetailsService userDetailsService) {
    this.jwtAuthFilter = jwtAuthFilter;
    this.userDetailsService = userDetailsService;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable()) // API sin cookies de sesión: se usa JWT en el header
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/auth/**")
                    .permitAll() // registro, login, confirmación, refresh y logout
                    .requestMatchers(
                        "/", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error")
                    .permitAll() // documentación y página de error
                    .requestMatchers(
                        "/user/getall", "/user/count", "/user/exists/**", "/user/getbyid/**")
                    .hasAnyRole("USER", "ADMIN")
                    .requestMatchers("/user/**", "/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (req, res, e) ->
                            writeJson(
                                res,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "No autorizado",
                                "Token ausente, inválido o vencido"))
                    .accessDeniedHandler(
                        (req, res, e) ->
                            writeJson(
                                res,
                                HttpServletResponse.SC_FORBIDDEN,
                                "Acceso prohibido",
                                "No tienes permisos para esta operación")))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /** Escribe una respuesta de error JSON con la misma forma que GlobalExceptionHandler. */
  private static void writeJson(HttpServletResponse res, int status, String error, String message)
      throws IOException {
    res.setStatus(status);
    res.setContentType("application/json;charset=UTF-8");
    res.getWriter().write("{\"error\":\"" + error + "\",\"mensaje\":\"" + message + "\"}");
  }

  /** CORS: permite que el front (por ejemplo http://localhost:5173) llame a la API. */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }
  

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  /** BCrypt con salt automático: las contraseñas se guardan solo como hash. */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
  }
}
