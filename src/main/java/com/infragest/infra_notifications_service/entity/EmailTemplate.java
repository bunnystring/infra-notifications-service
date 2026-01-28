package com.infragest.infra_notifications_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Tabla que almacena configuraciones de templates de correos electrónicos.
 * Permite definir mensajes y configuraciones de correo dinámicos.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
@Entity
@Table(name = "email_templates", indexes = {
        @Index(name = "idx_template_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EmailTemplate extends BaseEntity{

    /**
     * Nombre único del template para identificarlo (por ejemplo, "order_created").
     */
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Asunto del correo.
     */
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    /**
     * Contenido dinámico del cuerpo del correo.
     * Se pueden incluir placeholders como `{orderId}`, `{recipientEmail}`.
     */
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

}
