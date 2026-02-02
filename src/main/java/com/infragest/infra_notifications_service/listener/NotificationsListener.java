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
 * Maneja eventos de órdenes publicados desde `orders.exchange`.
 * Escucha diferentes estados de órdenes y delega el procesamiento a {@link NotificationService}.
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

        log.info("Mensaje recibido desde RabbitMQ para la orden ID: {}. Estado: {}", event.getOrderId(), event.getState());

        try {

            // Procesar el evento basado en el estado recibido
            switch (event.getState()) {
                case CREATED:
                    log.info("Procesando el estado CREATED para la orden ID: {}", event.getOrderId());
                    notificationService.processOrderCreatedEvent(event);
                    break;

                case IN_PROCESS:
                case DISPATCHED:
                    log.info("Procesando los estados IN_PROCESS o DISPATCHED para la orden ID: {}", event.getOrderId());
                    notificationService.processOrderUpdatedEvent(event);
                    break;

                case FINISHED:
                    log.info("Procesando el estado FINISHED para la orden ID: {}", event.getOrderId());
                    notificationService.processOrderFinishedEvent(event);
                    break;

                default:
                    log.warn("Estado desconocido recibido: {}. Ignorando el evento: {}", event.getState(), event);
            }

        } catch (Exception e) {
            log.error("Error al procesar el evento para la orden ID: {}. Estado: {}. Detalles del evento: {}",
                    event.getOrderId(), event.getState(), event, e);
        }
    }
}
