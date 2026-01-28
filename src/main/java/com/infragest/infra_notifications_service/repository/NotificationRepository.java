package com.infragest.infra_notifications_service.repository;

import com.infragest.infra_notifications_service.entity.Notification;
import com.infragest.infra_notifications_service.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para gestionar operaciones CRUD en la entidad Notification.
 * Incluye métodos personalizados para búsquedas específicas.
 *
 * @author
 * @since 2026-01-27
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Busca notificaciones asociadas a una orden específica.
     *
     * @param orderId ID de la orden asociada a las notificaciones.
     * @return Lista de notificaciones encontradas para la orden.
     */
    List<Notification> findByOrderId(UUID orderId);

    /**
     * Busca notificaciones por su estado (e.g., SENT, FAILED).
     *
     * @param status Estado de las notificaciones.
     * @return Lista de notificaciones con el estado especificado.
     */
    List<Notification> findByStatus(NotificationStatus status);

    /**
     * Busca una notificación específica por el destinatario y la orden asociada.
     *
     * @param orderId ID de la orden asociada.
     * @param recipientEmail Correo electrónico del destinatario.
     * @return Un Optional con la notificación encontrada, si existe.
     */
    Optional<Notification> findByOrderIdAndRecipientEmail(UUID orderId, String recipientEmail);
}

