package com.infragest.infra_notifications_service.service;

import com.infragest.infra_notifications_service.event.OrderEvent;

/**
 * Interfaz para el servicio de notificaciones, encargado de procesar
 * eventos relacionados con las órdenes.
 *
 * Proporciona métodos para manejar eventos de creación, actualización
 * y finalización de órdenes.
 *
 * @since 2026-01-27
 */
public interface NotificationService {

    /**
     * Procesa un evento de RabbitMQ que notifica la creación de una orden.
     *
     * @param orderEvent Evento con los detalles de la orden creada.
     *                   Contiene información como el ID de la orden y
     *                   los correos electrónicos de los destinatarios.
     */
    void processOrderCreatedEvent(OrderEvent orderEvent);

    /**
     * Procesa un evento de RabbitMQ que notifica la actualización de una orden.
     *
     * @param orderEvent Evento con los detalles de la orden actualizada.
     *                   Contiene información como el ID de la orden y
     *                   los correos electrónicos de los destinatarios.
     */
    void processOrderUpdatedEvent(OrderEvent orderEvent);


    /**
     * Procesa un evento que notifica la finalización de una orden.
     *
     * @param orderEvent Evento con los detalles de la orden finalizada.
     *                   Contiene información como el ID de la orden y
     *                   los correos electrónicos de los destinatarios.
     */
    void processOrderFinishedEvent(OrderEvent orderEvent);
}
