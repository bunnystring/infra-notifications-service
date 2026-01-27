package com.infragest.infra_notifications_service.entity;

import com.infragest.infra_notifications_service.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_order_id", columnList = "order_id"),
        @Index(name = "idx_notification_status", columnList = "status"),
        @Index(name = "idx_notification_email", columnList = "recipient_email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Notification extends BaseEntity {

    /**
     * ID único de la orden asociada a esta notificación.
     */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /**
     * Dirección de correo electrónico del destinatario.
     */
    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    /**
     * Estado actual de la notificación (SENT, FAILED, PENDING).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private NotificationStatus status;

    /**
     * Mensaje relacionado con el estado, como una descripción
     * del resultado del envío o del error producido.
     */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

}
