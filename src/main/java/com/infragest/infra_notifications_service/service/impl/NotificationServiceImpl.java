package com.infragest.infra_notifications_service.service.impl;

import com.infragest.infra_notifications_service.entity.EmailTemplate;
import com.infragest.infra_notifications_service.entity.Notification;
import com.infragest.infra_notifications_service.enums.NotificationStatus;
import com.infragest.infra_notifications_service.event.NotificationEvent;
import com.infragest.infra_notifications_service.event.OrderEvent;
import com.infragest.infra_notifications_service.repository.EmailTemplateRepository;
import com.infragest.infra_notifications_service.repository.NotificationRepository;
import com.infragest.infra_notifications_service.service.NotificationService;
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
     * Constructor con los parámetros necesarios para la inicialización del servicio de notificaciones.
     *
     * @param rabbitMQConfig RabbitTemplate configurado para interactuar con RabbitMQ.
     * @param notificationRepository Repositorio utilizado para persistir notificaciones en la base de datos.
     * @param emailTemplateRepository Repositorio utilizado para buscar templates de correo.
     */
    public NotificationServiceImpl(RabbitTemplate rabbitMQConfig, NotificationRepository notificationRepository, EmailTemplateRepository emailTemplateRepository) {
        this.rabbitTemplate = rabbitMQConfig;
        this.notificationRepository = notificationRepository;
        this.emailTemplateRepository = emailTemplateRepository;
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
            saveNotification(orderEvent.getOrderId(), null, NotificationStatus.FAILED, "Sin destinatarios para la notificación.", null);
            sendConfirmation(orderEvent, "FAILED");
            return;
        }

        // Obtener el template una vez antes del loop
        Optional<EmailTemplate> optionalTemplate = emailTemplateRepository.findByName("order_created");

        if (optionalTemplate.isEmpty()) {
            log.warn("Template 'order_created' no encontrado. Degradando las notificaciones a estado 'FAILED'.");

            // Registrar un estado general como FALLIDO sin intentar enviar notificaciones
            orderEvent.getRecipientEmails().forEach(email ->
                    saveNotification(orderEvent.getOrderId(), email, NotificationStatus.FAILED, "Template de correo no disponible.", null)
            );

            // Publicar confirmación de falla
            sendConfirmation(orderEvent, "FAILED");
            return;
        }

        EmailTemplate template = optionalTemplate.get();

        // Enviar notificaciones a los destinatarios
        orderEvent.getRecipientEmails().forEach(email -> sendNotification(orderEvent, email, template));

        // Publicar confirmación general como éxito si al menos un correo se envió
        sendConfirmation(orderEvent, "SUCCESS");
    }

    /**
     * Envía una notificación a un correo específico y registra el resultado.
     *
     * @param orderEvent Evento que contiene el ID de la orden y otros datos relevantes.
     * @param recipientEmail Correo electrónico del destinatario.
     */
    private void sendNotification(OrderEvent orderEvent, String recipientEmail, EmailTemplate template) {
        log.info("Enviando notificación a: {} para la orden ID: {}", recipientEmail, orderEvent.getOrderId());

        try {
            // Simular lógica de envío de correo (esto sería la integración real con un cliente de correos)



            log.info("Correo enviado satisfactoriamente a: {}", recipientEmail);
            saveNotification(orderEvent.getOrderId(), recipientEmail, NotificationStatus.SENT, "Correo enviado exitosamente.", template);
        } catch (Exception ex) {
            log.error("Error enviando correo a: {} para la orden ID: {}", recipientEmail, orderEvent.getOrderId(), ex);
            saveNotification(orderEvent.getOrderId(), recipientEmail, NotificationStatus.FAILED, "Error al enviar correo: " + ex.getMessage(), template);
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
     * @param status Estado de la notificación (SENT, FAILED, PENDING).
     * @param message Mensaje adicional relacionado con el envío.
     */
    public void saveNotification(UUID orderId, String recipientEmail, NotificationStatus status, String message, EmailTemplate template) {

        // Crear la notificación, asociando el template
        Notification notification = Notification.builder()
                .orderId(orderId)
                .recipientEmail(recipientEmail)
                .status(status)
                .message(message)
                .template(template)
                .build();

        notificationRepository.save(notification);
        log.info("Notificación registrada: Orden ID: {}, Email: {}, Estado: {}, Mensaje: {}",
                orderId, recipientEmail, status, message);
    }
}
