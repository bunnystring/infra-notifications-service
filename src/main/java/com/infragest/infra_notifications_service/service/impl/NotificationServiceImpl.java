package com.infragest.infra_notifications_service.service.impl;

import com.infragest.infra_notifications_service.entity.EmailTemplate;
import com.infragest.infra_notifications_service.entity.Notification;
import com.infragest.infra_notifications_service.enums.NotificationStatus;
import com.infragest.infra_notifications_service.event.NotificationEvent;
import com.infragest.infra_notifications_service.event.OrderEvent;
import com.infragest.infra_notifications_service.repository.EmailTemplateRepository;
import com.infragest.infra_notifications_service.repository.NotificationRepository;
import com.infragest.infra_notifications_service.service.NotificationService;
import com.infragest.infra_notifications_service.service.SendGridEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementación del servicio de notificaciones para manejar eventos relacionados con órdenes.
 *
 * @author bunnystring
 * @since 2026-01-27
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    /**
     * RabbitTemplate: utilizado para interactuar con RabbitMQ.
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * NotificationRepository: repositorio de notification.
     */
    private final NotificationRepository notificationRepository;

    /**
     * EmailTemplateRepository: repositorio de template email.
     */
    private final EmailTemplateRepository emailTemplateRepository;

    /**
     * SendGridEmailService: servicio de sendGridEmailService.
     */
    private final SendGridEmailService sendGridEmailService;

    /**
     * Constructor con los parámetros necesarios para la inicialización del servicio de notificaciones.
     *
     * @param rabbitMQConfig RabbitTemplate configurado para interactuar con RabbitMQ.
     * @param notificationRepository Repositorio utilizado para persistir notificaciones en la base de datos.
     * @param emailTemplateRepository Repositorio utilizado para buscar templates de correo.
     */
    public NotificationServiceImpl(RabbitTemplate rabbitMQConfig, NotificationRepository notificationRepository, EmailTemplateRepository emailTemplateRepository, SendGridEmailService sendGridEmailService) {
        this.rabbitTemplate = rabbitMQConfig;
        this.notificationRepository = notificationRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.sendGridEmailService = sendGridEmailService;
    }

    /**
     * Escucha mensajes desde la cola de RabbitMQ y procesa los eventos.
     * Este método se ejecuta cada vez que RabbitMQ envía un mensaje a la cola.
     *
     * @param orderEvent Evento que notifica la creación de una orden.
     */
    @Override
    @RabbitListener(queues = "notifications.queue")
    public void processOrderCreatedEvent(OrderEvent orderEvent) {

        log.info("Procesando evento de orden ID: {}", orderEvent.getOrderId());

        if (orderEvent.getRecipientEmails() == null || orderEvent.getRecipientEmails().isEmpty()) {
            log.warn("No se encontraron destinatarios para la orden ID: {}", orderEvent.getOrderId());
            saveNotification(orderEvent.getOrderId(), null, NotificationStatus.FAILED, "Sin destinatarios para la notificación.");
            sendConfirmation(orderEvent, "FAILED");
            return;
        }

        // Consultar la plantilla UNA SOLA VEZ antes de iterar los destinatarios
        Optional<EmailTemplate> optionalTemplate = emailTemplateRepository.findByName("order_created");
        if (optionalTemplate.isEmpty()) {
            log.warn("La plantilla 'order_created' no se encontró. No se enviará ninguna notificación para la orden ID: {}", orderEvent.getOrderId());
            sendConfirmation(orderEvent, "FAILED");
            return; // Si no hay plantilla, no continúes con los envíos
        }
        EmailTemplate template = optionalTemplate.get();

        // Enviar notificaciones a los destinatarios
        orderEvent.getRecipientEmails().forEach(email -> {sendNotification(template, orderEvent, email);});

        // Publicar confirmación general como éxito si al menos un correo se envió
        sendConfirmation(orderEvent, "SUCCESS");
    }

    /**
     * Envía una notificación a un correo específico y registra el resultado.
     *
     * @param orderEvent Evento que contiene el ID de la orden y otros datos relevantes.
     * @param recipientEmail Correo electrónico del destinatario.
     */
    private void sendNotification(EmailTemplate template, OrderEvent orderEvent, String recipientEmail) {

        UUID orderId = orderEvent.getOrderId();
        log.info("Enviando notificación a: {} para la orden ID: {}", recipientEmail, orderId);

        // Construir el contenido del correo usando la plantilla
        String subject = template.getSubject();
        String content = template.getBody()
                .replace("{orderId}", orderId.toString())
                .replace("{recipientEmail}", recipientEmail);

        try {
            sendGridEmailService.sendEmail(recipientEmail, subject, content); // Enviar el correo con SendGrid
            saveNotification(orderEvent.getOrderId(), recipientEmail, NotificationStatus.SENT, "Correo enviado exitosamente.");
        } catch (Exception ex) {
            log.error("Fallo al enviar correo para ID de orden: {}, destinatario: {}", orderEvent.getOrderId(), recipientEmail, ex);
            saveNotification(orderEvent.getOrderId(), recipientEmail, NotificationStatus.FAILED, ex.getMessage());
        }
    }


    /**
     * Publica una confirmación en RabbitMQ después de procesar un evento de notificación.
     *
     * @param event  Evento recibido que contiene los datos básicos de la orden.
     * @param status Estado de la notificación procesada. Por ejemplo: "SUCCESS" o "FAILED".
     */
    private void sendConfirmation(OrderEvent event, String status) {
        NotificationEvent notificationEvent = new NotificationEvent(event.getOrderId(), status, "Estado: " + status);
        rabbitTemplate.convertAndSend("notifications.exchange", "notification.completed", notificationEvent);
        log.info("Confirmación publicada: {}", notificationEvent);
    }

    /**
     * Procesa un evento de RabbitMQ que notifica la actualización de una orden.
     *
     * @param orderEvent Evento con los detalles de la orden actualizada.
     */
    @Override
    public void processOrderUpdatedEvent(OrderEvent orderEvent) {

    }

    /**
     * Guarda el resultado de una notificación en la base de datos.
     *
     * @param orderId ID de la orden asociada.
     * @param recipientEmail Correo electrónico del destinatario.
     * @param status Estado de la notificación.
     * @param message Mensaje adicional relacionado con el envío.
     */
    public void saveNotification(UUID orderId, String recipientEmail, NotificationStatus status, String message) {

        Optional<EmailTemplate> optionalTemplate = emailTemplateRepository.findByName("order_created");
        EmailTemplate template = optionalTemplate.orElseThrow(() ->
                new RuntimeException("No se encontró un template válido para esta notificación."));

        Notification notification = Notification.builder()
                .orderId(orderId)
                .recipientEmail(recipientEmail)
                .status(status)
                .message(message)
                .template(template)
                .build();

        notificationRepository.save(notification);
        log.info("Notificación registrada: Orden ID: {}, Email: {}, Estado: {}", orderId, recipientEmail, status);
    }
}
