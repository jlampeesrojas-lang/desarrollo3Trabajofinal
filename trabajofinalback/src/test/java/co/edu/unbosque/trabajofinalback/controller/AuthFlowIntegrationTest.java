package co.edu.unbosque.trabajofinalback.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.trabajofinalback.model.AuditAction;
import co.edu.unbosque.trabajofinalback.model.User;
import co.edu.unbosque.trabajofinalback.repository.AuditLogRepository;
import co.edu.unbosque.trabajofinalback.repository.UserRepository;
import co.edu.unbosque.trabajofinalback.service.EmailService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
/**
 * Pruebas de integración (H2 en memoria, sin correo real) que demuestran lo que se muestra en
 * Postman: 401 sin token, 403 de USER en ruta admin, token vencido rechazado, registro con
 * confirmación, refresh con rotación y auditoría.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

  private static final String PASSWORD = "Clave#12345";

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper om;
  @Autowired private UserRepository userRepo;
  @Autowired private AuditLogRepository auditRepo;

  @Value("${jwt.secret}")
  private String jwtSecret;

  @MockitoBean private EmailService emailService;

  // ------------------------------------------------------------ utilidades

  private ResultActions postJson(String url, Object body, String bearer) throws Exception {
    var req = post(url).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body));
    if (bearer != null) {
      req = req.header("Authorization", "Bearer " + bearer);
    }
    return mvc.perform(req);
  }

  private JsonNode json(ResultActions result) throws Exception {
    return om.readTree(result.andReturn().getResponse().getContentAsString());
  }

  private String loginAndGetAccessToken(String identifier, String password) throws Exception {
    return json(postJson("/auth/login", Map.of("identifier", identifier, "password", password), null)
            .andExpect(status().isOk()))
        .get("accessToken")
        .asText();
  }

  // ----------------------------------------------------------------- pruebas

  @Test
  void sinTokenLasRutasProtegidasDevuelven401() throws Exception {
    mvc.perform(get("/user/getall")).andExpect(status().isUnauthorized());
    mvc.perform(get("/cualquier/ruta")).andExpect(status().isUnauthorized());
  }

  @Test
  void tokenInventadoDevuelve401() throws Exception {
    mvc.perform(get("/user/getall").header("Authorization", "Bearer esto.no.es.un.jwt"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void tokenVencidoSeRechazaCon401() throws Exception {
    String vencido =
        Jwts.builder()
            .setSubject("admin")
            .setExpiration(new Date(System.currentTimeMillis() - 60_000))
            .signWith(
                Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)),
                SignatureAlgorithm.HS256)
            .compact();
    mvc.perform(get("/user/getall").header("Authorization", "Bearer " + vencido))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void registroConContrasenaDebilDevuelve400() throws Exception {
    postJson(
            "/auth/register",
            Map.of("username", "debil1", "email", "debil1@correo.com", "password", "abc12345"),
            null)
        .andExpect(status().isBadRequest());
  }

  @Test
  void registroConCorreoInvalidoDevuelve400() throws Exception {
    postJson(
            "/auth/register",
            Map.of("username", "malcorreo", "email", "no-es-correo", "password", PASSWORD),
            null)
        .andExpect(status().isBadRequest());
  }

  @Test
  void flujoCompletoRegistroConfirmacionLoginYRefresh() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String username = "user" + suffix;
    String email = username + "@correo.com";

    // 1. Registro: 201, hash BCrypt en la base y correo enviado
    postJson("/auth/register", Map.of("username", username, "email", email, "password", PASSWORD), null)
        .andExpect(status().isCreated());
    User saved = userRepo.findByUsername(username).orElseThrow();
    assertTrue(saved.getPasswordHash().startsWith("$2a$"), "La contraseña debe guardarse como hash BCrypt");
    assertTrue(!saved.getPasswordHash().contains(PASSWORD));
    assertTrue(!saved.isEmailVerified());

    ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailService).sendConfirmationEmail(any(User.class), tokenCaptor.capture());
    String emailToken = tokenCaptor.getValue();

    // 2. Usuario repetido: 409
    postJson("/auth/register", Map.of("username", username, "email", "otro" + suffix + "@correo.com", "password", PASSWORD), null)
        .andExpect(status().isConflict());

    // 3. Login antes de confirmar: 403
    postJson("/auth/login", Map.of("identifier", email, "password", PASSWORD), null)
        .andExpect(status().isForbidden());

    // 4. Confirmar: 200; reusar el enlace: 400
    postJson("/auth/confirm-email", Map.of("token", emailToken), null).andExpect(status().isOk());
    postJson("/auth/confirm-email", Map.of("token", emailToken), null)
        .andExpect(status().isBadRequest());

    // 5. Contraseña incorrecta: 401
    postJson("/auth/login", Map.of("identifier", email, "password", "Incorrecta#1"), null)
        .andExpect(status().isUnauthorized());

    // 6. Login correcto con correo: 200, access + refresh token, sin contraseña en la respuesta
    JsonNode login =
        json(
            postJson("/auth/login", Map.of("identifier", email, "password", PASSWORD), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER")));
    String access = login.get("accessToken").asText();
    String refresh = login.get("refreshToken").asText();

    // 7. Con token entra a rutas de USER (202 en /user/count)
    mvc.perform(get("/user/count").header("Authorization", "Bearer " + access))
        .andExpect(status().isAccepted());

    // 8. Refresh con rotación: el nuevo funciona, el viejo ya no
    JsonNode refreshed =
        json(postJson("/auth/refresh", Map.of("refreshToken", refresh), null).andExpect(status().isOk()));
    String newRefresh = refreshed.get("refreshToken").asText();
    postJson("/auth/refresh", Map.of("refreshToken", refresh), null).andExpect(status().isUnauthorized());

    // 9. Reusar el viejo revocó todas las sesiones: el nuevo tampoco sirve
    postJson("/auth/refresh", Map.of("refreshToken", newRefresh), null).andExpect(status().isUnauthorized());

    // 10. Auditoría
    assertTrue(auditRepo.countByUserIdAndAction(saved.getId(), AuditAction.REGISTER) == 1);
    assertTrue(auditRepo.countByUserIdAndAction(saved.getId(), AuditAction.EMAIL_CONFIRMED) == 1);
    assertTrue(auditRepo.countByUserIdAndAction(saved.getId(), AuditAction.LOGIN_OK) == 1);
    assertTrue(auditRepo.countByUserIdAndAction(saved.getId(), AuditAction.LOGIN_FAIL) >= 2);
    assertTrue(auditRepo.countByUserIdAndAction(saved.getId(), AuditAction.TOKEN_REUSE_DETECTED) >= 1);
  }

  @Test
  void logoutRevocaElRefreshToken() throws Exception {
    JsonNode login =
        json(
            postJson("/auth/login", Map.of("identifier", "normaluser", "password", "Admin#2026Chat"), null)
                .andExpect(status().isOk()));
    String refresh = login.get("refreshToken").asText();

    postJson("/auth/logout", Map.of("refreshToken", refresh), null).andExpect(status().isOk());
    postJson("/auth/refresh", Map.of("refreshToken", refresh), null).andExpect(status().isUnauthorized());
  }

  @Test
  void userNormalRecibe403EnRutaDeAdmin() throws Exception {
    String token = loginAndGetAccessToken("user@chat.local", "Admin#2026Chat");
    Map<String, String> body =
        Map.of("username", "x" + UUID.randomUUID().toString().substring(0, 6), "email", "x@x.com", "password", PASSWORD);
    postJson("/user/createjson", body, token).andExpect(status().isForbidden());
  }

  @Test
  void adminPuedeCrearUsuarioYLaRespuestaNoTraeContrasenas() throws Exception {
    String token = loginAndGetAccessToken("admin", "Admin#2026Chat");
    String name = "adm" + UUID.randomUUID().toString().substring(0, 6);
    postJson(
            "/user/createjson",
            Map.of("username", name, "email", name + "@correo.com", "password", PASSWORD, "role", "USER"),
            token)
        .andExpect(status().isCreated());

    String body =
        mvc.perform(get("/user/getall").header("Authorization", "Bearer " + token))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertTrue(body.contains(name));
    assertTrue(!body.contains("password"), "La respuesta no debe incluir contraseñas ni hashes");

    // El usuario creado por el admin ya tiene el correo verificado y puede entrar
    loginAndGetAccessToken(name, PASSWORD);
  }
}
