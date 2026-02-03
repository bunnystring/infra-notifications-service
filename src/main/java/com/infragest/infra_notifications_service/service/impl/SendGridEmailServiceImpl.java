package com.infragest.infra_notifications_service.service.impl;

import com.infragest.infra_notifications_service.exception.SengridException;
import com.infragest.infra_notifications_service.service.SendGridEmailService;
import com.infragest.infra_notifications_service.util.MessageException;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Implementación del servicio de envío de correos utilizando la API de SendGrid.
 */
@Service
public class SendGridEmailServiceImpl implements SendGridEmailService {

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.default-from-email}")
    private String fromEmail;

    /**
     * Envía un correo electrónico a un destinatario utilizando la API de SendGrid.
     *
     * @param recipientEmail Dirección de correo del destinatario.
     * @param subject        Asunto del mensaje.
     * @param content        Cuerpo del mensaje en texto plano.
     */
    @Override
    public void sendEmail(String recipientEmail, String subject, String content) {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new SengridException(MessageException.SENDER_EMAIL_NOT_CONFIGURED, SengridException.Type.INVALID_REQUEST);
        }

        // Configurar remitente
        Email from = new Email(fromEmail);

        // Configurar destinatario
        Email to = new Email(recipientEmail);

        // Contenido del correo
        Content emailContent = new Content("text/plain", content);

        // Crear el correo
        Mail mail = new Mail(from, subject, to, emailContent);

        // Iniciar la conexión con la API Key
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {

            // Configurar el request de envío
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            // Validar códigos de respuesta de SendGrid
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                // El correo fue enviado exitosamente
                return;
            } else {
                // Manejo de errores por parte de SendGrid
                throw new SengridException(String.format(MessageException.SENDGRID_API_ERROR,
                        response.getStatusCode(),
                        response.getBody()), SengridException.Type.API_ERROR);
            }
        } catch (IOException ex) {
            // Manejo de errores relacionados con la conexión o la IO
            throw new SengridException(String.format(MessageException.SENDGRID_CONNECTION_FAILURE, ex.getMessage()), SengridException.Type.SERVICE_UNAVAILABLE);
        }
    }
}
