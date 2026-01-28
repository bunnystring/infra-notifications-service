package com.infragest.infra_notifications_service.entity;

import com.infragest.infra_notifications_service.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad que representa una notificación enviada por el sistema.
 * Cada notificación contiene información sobre el destinatario, estado del envío
 * y el template utilizado (obligatorio).
 *
 * Extiende {@link BaseEntity} para reutilizar atributos base como IDs y timestamps.
 * Está relacionada con {@link EmailTemplate} para registrar el template utilizado.
 *
 * @author
 * @since 2026-01-27
 */
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
    @Column(name = "recipient_email", length = 255)
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

    /**
     * Relación con {@link EmailTemplate}.
     * Esta relación es obligatoria: cada notificación debe estar asociada a un template.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private EmailTemplate template;

}
