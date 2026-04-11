package pe.com.carlosh.tallyapi.notification;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final Resend resend;
    private final String fromEmail;

    public EmailService(
            @Value("${resend.api.key}") String apiKey,
            @Value("${resend.from.email}") String fromEmail) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    @Async
    public void sendVerificationEmail(String toEmail, String token) {
        String verificationLink = "https://tallygastos.lat/verify?token=" + token;

        String htmlBody = String.format(
                "<h2>Bienvenido a Tally</h2>" +
                        "<p>Gracias por registrarte. Para empezar a gestionar tus presupuestos, por favor verifica tu cuenta haciendo clic en el botón de abajo:</p>" +
                        "<a href='%s' style='background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;'>Verificar Cuenta</a>" +
                        "<p>Si el botón no funciona, copia y pega este enlace en tu navegador:</p>" +
                        "<p><a href='%s'>%s</a></p>",
                verificationLink, verificationLink, verificationLink
        );

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Tally API <" + fromEmail + ">")
                .to(toEmail)
                .subject("Verifica tu cuenta en Tally")
                .html(htmlBody)
                .build();

        try {
            resend.emails().send(params);
            log.info("Correo de verificación enviado asíncronamente a: {}", toEmail);
        } catch (ResendException e) {
            log.error("Error al enviar el correo de verificación con Resend: {}", e.getMessage());
        }
    }
}