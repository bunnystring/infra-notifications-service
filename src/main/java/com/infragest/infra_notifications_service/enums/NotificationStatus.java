package com.infragest.infra_notifications_service.enums;

/**
 * Enum que define los estados posibles de una notificación.
 * Utilizado en la entidad {@link com.infragest.infra_notifications_service.entity.Notification}.
 *
 * @author
 * @since 2026-01-27
 */
public enum NotificationStatus {
    SENT, // Notificación enviada exitosamente.
    FAILED, // Intento de notificación fallido.
    PENDING // Notificación pendiente de envío.
}
