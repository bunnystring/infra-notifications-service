package com.infragest.infra_notifications_service.listener;

import com.infragest.infra_notifications_service.config.RabbitMQConfig;
import com.infragest.infra_notifications_service.event.OrderEvent;
import com.infragest.infra_notifications_service.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener para procesar mensajes de RabbitMQ relacionados con órdenes.
 *
 * @author bunnystring
 * @since 2026-01-27
 */
@Slf4j
@Component
public class NotificationsListener {

    /**
     * NotificationService: inyeccion de servicio notificationService.
     */
    private final NotificationService notificationService;

    /**
     * Constructor del listener con inyección del servicio de notificaciones.
     *
     * @param notificationService Servicio encargado de procesar los eventos de órdenes.
     */
    public NotificationsListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Escucha y procesa mensajes desde la cola configurada en RabbitMQ.
     *
     * @param event Evento recibido desde RabbitMQ que contiene los detalles de la orden.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleOrderEvent(OrderEvent event) {

        log.info("Mensaje recibido: " + event);

        // Procesar el evento delegándolo al servicio
        try {
            notificationService.processOrderCreatedEvent(event);
        } catch (Exception e) {
            log.error("Error al procesar el evento OrderEvent: {}", event, e);
        }
    }
}
