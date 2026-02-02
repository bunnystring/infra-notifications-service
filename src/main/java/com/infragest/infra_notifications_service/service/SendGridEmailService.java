package com.infragest.infra_notifications_service.service;

/**
 * Contrato para definir un servicio de envío de correos electrónicos utilizando SendGrid.
 * Este servicio proporciona funcionalidades genéricas para enviar correos a destinatarios
 * específicos con un asunto y contenido dinámico.
 *
 * Implementaciones posibles incluyen conexiones con la API de SendGrid o algún sistema externo.
 *
 * @author
 * @since 2026-01-27
 */
public interface SendGridEmailService {

    /**
     * Envía un correo electrónico a un destinatario.
     *
     * @param recipientEmail Dirección de correo del destinatario.
     * @param subject Asunto del mensaje.
     * @param content Cuerpo del mensaje en texto plano.
     */
    void sendEmail(String recipientEmail, String subject, String content);

}
