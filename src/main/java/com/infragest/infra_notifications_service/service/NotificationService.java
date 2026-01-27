package com.infragest.infra_notifications_service.service;

import com.infragest.infra_notifications_service.event.OrderEvent;

public interface NotificationService {

    /**
     * Procesa un evento de RabbitMQ que notifica la creación de una orden.
     *
     * @param orderEvent Evento con los detalles de la orden creada.
     */
    void processOrderCreatedEvent(OrderEvent orderEvent);

    /**
     * Procesa un evento de RabbitMQ que notifica la actualización de una orden.
     *
     * @param orderEvent Evento con los detalles de la orden actualizada.
     */
    void processOrderUpdatedEvent(OrderEvent orderEvent);
}
