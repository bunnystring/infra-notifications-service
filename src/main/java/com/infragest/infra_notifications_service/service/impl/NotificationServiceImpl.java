package com.infragest.infra_notifications_service.service.impl;

import com.infragest.infra_notifications_service.event.NotificationEvent;
import com.infragest.infra_notifications_service.event.OrderEvent;
import com.infragest.infra_notifications_service.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

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
     * Constructor con los parametros de la clase.
     *
     * @param rabbitMQConfig RabbitTemplate configurado para la interacción con RabbitMQ.
     */
    public NotificationServiceImpl(RabbitTemplate rabbitMQConfig) {
        this.rabbitTemplate = rabbitMQConfig;
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
        log.info("Procesando evento: {}", orderEvent);

        try {
            sendNotification(orderEvent);
        } catch (Exception ex) {
            log.error("Error procesando la notificación para la orden ID: {}", orderEvent.getOrderId(), ex);
            throw ex;
        }
    }

    /**
     * Envía una notificación basada en el evento recibido.
     *
     * @param orderEvent Evento que contiene los datos relevantes para la notificación.
     */
    private void sendNotification(OrderEvent orderEvent) {
        log.info("Enviando notificación para la orden ID: {}", orderEvent.getOrderId());

        try {
            // Lógica real de envío
            log.info("Notificación enviada para la orden ID: {}", orderEvent.getOrderId());
            sendConfirmation(orderEvent, "SUCCESS");
        } catch (Exception ex) {
            log.error("Fallo en el envío de la notificación para la orden ID: {}", orderEvent.getOrderId());
            sendConfirmation(orderEvent, "FAILED");
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
}
