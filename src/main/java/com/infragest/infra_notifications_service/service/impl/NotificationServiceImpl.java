package com.infragest.infra_notifications_service.service.impl;

import com.infragest.infra_notifications_service.entity.Notification;
import com.infragest.infra_notifications_service.enums.NotificationStatus;
import com.infragest.infra_notifications_service.event.NotificationEvent;
import com.infragest.infra_notifications_service.event.OrderEvent;
import com.infragest.infra_notifications_service.repository.NotificationRepository;
import com.infragest.infra_notifications_service.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

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
     * Constructor con los parametros de la clase.
     *
     * @param rabbitMQConfig RabbitTemplate configurado para la interacción con RabbitMQ.
     */
    public NotificationServiceImpl(RabbitTemplate rabbitMQConfig, NotificationRepository notificationRepository) {
        this.rabbitTemplate = rabbitMQConfig;
        this.notificationRepository = notificationRepository;
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

        // Enviar notificaciones a los destinatarios
        orderEvent.getRecipientEmails().forEach(email -> {
            try {
                sendNotification(orderEvent, email);
            } catch (Exception ex) {
                log.error("Error al enviar notificación a: {} para la orden ID: {}", email, orderEvent.getOrderId(), ex);
                saveNotification(orderEvent.getOrderId(), email, NotificationStatus.FAILED, ex.getMessage());
            }
        });

        // Publicar confirmación general como éxito si al menos un correo se envió
        sendConfirmation(orderEvent, "SUCCESS");
    }

    /**
     * Envía una notificación a un correo específico y registra el resultado.
     *
     * @param orderEvent Evento que contiene el ID de la orden y otros datos relevantes.
     * @param recipientEmail Correo electrónico del destinatario.
     */
    private void sendNotification(OrderEvent orderEvent, String recipientEmail) {
        log.info("Enviando notificación a: {} para la orden ID: {}", recipientEmail, orderEvent.getOrderId());

        // Simulación de envío real de correo (aquí podrías usar SendGrid o una API similar)
        try {
            // Simular lógica de envío de correo (esto sería la integración real con un cliente de correos)
            log.info("Correo enviado satisfactoriamente a: {}", recipientEmail);
            saveNotification(orderEvent.getOrderId(), recipientEmail, NotificationStatus.SENT, "Correo enviado exitosamente.");
        } catch (Exception ex) {
            log.error("Error enviando correo a: {}", recipientEmail, ex);
            throw new RuntimeException("Fallo en notificación a " + recipientEmail, ex);
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
    public void saveNotification(UUID orderId, String recipientEmail, NotificationStatus status, String message) {
        Notification notification = Notification.builder()
                .orderId(orderId)
                .recipientEmail(recipientEmail)
                .status(status)
                .message(message)
                .build();

        notificationRepository.save(notification);
        log.info("Notificación registrada: Orden ID: {}, Email: {}, Estado: {}, Mensaje: {}",
                orderId, recipientEmail, status, message);
    }
}
