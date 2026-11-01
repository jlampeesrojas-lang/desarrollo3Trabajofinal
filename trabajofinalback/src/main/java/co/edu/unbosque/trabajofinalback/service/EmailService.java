package co.edu.unbosque.trabajofinalback.service;

import co.edu.unbosque.trabajofinalback.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Envío de correos automáticos con Jakarta Mail (a través de JavaMailSender de Spring). El servidor
 * SMTP se configura con las variables de entorno MAIL_HOST, MAIL_PORT, MAIL_USERNAME y
 * MAIL_PASSWORD.
 */
@Service
public class EmailService {

  private final JavaMailSender mailSender;

  /** Remitente del correo. */
  @Value("${app.mail.from}")
  private String from;

  /** URL base del front; el enlace del correo apunta a una ruta del front. */
  @Value("${app.frontend-url}")
  private String frontendUrl;

  public EmailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  /**
   * Envía el correo con el enlace para confirmar la cuenta.
   *
   * @param user Usuario recién registrado
   * @param rawToken Token en texto plano (solo viaja por este correo)
   * @throws org.springframework.mail.MailException si el servidor SMTP falla
   */
  public void sendConfirmationEmail(User user, String rawToken) {
    String link = frontendUrl + "/confirm-email?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    String name = HtmlUtils.htmlEscape(user.getUsername());
    String html =
        "<div style=\"font-family:Arial,sans-serif;max-width:480px\">"
            + "<h2>¡Bienvenido, " + name + "!</h2>"
            + "<p>Para activar tu cuenta confirma tu correo con el siguiente botón:</p>"
            + "<p><a href=\"" + link + "\" style=\"background:#D90012;color:#fff;padding:12px 20px;"
            + "text-decoration:none;border-radius:8px\">Confirmar correo</a></p>"
            + "<p>El enlace vence en 24 horas y solo puede usarse una vez.</p>"
            + "<p>Si no creaste esta cuenta, ignora este mensaje.</p></div>";
    send(user.getEmail(), "Confirma tu correo", html);
  }

  private void send(String to, String subject, String html) {
    MimeMessage message = mailSender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(html, true);
    } catch (MessagingException e) {
      throw new MailPreparationException("No se pudo preparar el correo", e);
    }
    mailSender.send(message);
  }
}
