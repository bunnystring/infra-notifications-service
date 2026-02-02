package com.infragest.infra_notifications_service.service.impl;

import com.infragest.infra_notifications_service.entity.EmailTemplate;
import com.infragest.infra_notifications_service.entity.Notification;
import com.infragest.infra_notifications_service.enums.NotificationStatus;
import com.infragest.infra_notifications_service.event.NotificationEvent;
import com.infragest.infra_notifications_service.event.OrderEvent;
import com.infragest.infra_notifications_service.repository.EmailTemplateRepository;
import com.infragest.infra_notifications_service.repository.NotificationRepository;
import com.infragest.infra_notifications_service.service.NotificationService;
import com.infragest.infra_notifications_service.service.SendGridEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

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
     * NotificationRepository: repositorio de notification.
     */
    private final NotificationRepository notificationRepository;

    /**
     * EmailTemplateRepository: repositorio de template email.
     */
    private final EmailTemplateRepository emailTemplateRepository;

    /**
     * SendGridEmailService: servicio de sendGridEmailService.
     */
    private final SendGridEmailService sendGridEmailService;

    /**
     * Constructor del servicio de notificaciones.
     *
     * @param rabbitTemplate          RabbitTemplate configurado para RabbitMQ.
     * @param notificationRepository  Repositorio usado para persistir notificaciones.
     * @param emailTemplateRepository Repositorio usado para buscar plantillas de correo.
     * @param sendGridEmailService    Servicio externo para envío de correos.
     */
    public NotificationServiceImpl(RabbitTemplate rabbitMQConfig, NotificationRepository notificationRepository, EmailTemplateRepository emailTemplateRepository, SendGridEmailService sendGridEmailService) {
        this.rabbitTemplate = rabbitMQConfig;
        this.notificationRepository = notificationRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.sendGridEmailService = sendGridEmailService;
    }

    /**
     * Procesa un evento de creación de una orden.
     *
     * Este método ingresa al flujo cuando una nueva orden es creada (estado CREATED).
     * Busca una plantilla de correo, y envía correos a los destinatarios de la orden.
     * También publica una confirmación en RabbitMQ del estado de las notificaciones.
     *
     * @param orderEvent Evento recibido que contiene detalles de la orden creada.
     */
    @Override
    public void processOrderCreatedEvent(OrderEvent orderEvent) {
        log.info("Procesando evento de orden - Estado: CREATED. Orden ID: {}", orderEvent.getOrderId());

        // Procesar las notificaciones usando la plantilla `order_created`
        processOrderEventWithTemplate(orderEvent, "order_created");
    }

    /**
     * Procesa un evento para órdenes actualizadas.
     *
     * Este método gestiona estados intermedios como:
     * - IN_PROCESS: La orden está en proceso.
     * - DISPATCHED: La orden ha sido enviada.
     *
     * Utiliza la plantilla `order_updated` para generar los correos.
     *
     * @param orderEvent Evento recibido que describe la actualización de una orden.
     */
    @Override
    public void processOrderUpdatedEvent(OrderEvent orderEvent) {
        log.info("Procesando evento de orden - Estado: UPDATED (IN_PROCESS/DISPATCHED). Orden ID: {}", orderEvent.getOrderId());

        // Procesar las notificaciones usando la plantilla `order_updated`
        processOrderEventWithTemplate(orderEvent, "order_updated");
    }

    /**
     * Procesa un evento para órdenes finalizadas.
     *
     * Este método es invocado cuando una orden alcanza el estado FINISHED.
     * Utiliza la plantilla `order_finished` para contactar a los destinatarios
     * y notificar que la orden ha sido completada.
     *
     * @param orderEvent Evento recibido que describe la finalización de una orden.
     */
    @Override
    public void processOrderFinishedEvent(OrderEvent orderEvent) {
        log.info("Procesando evento de orden - Estado: FINISHED. Orden ID: {}", orderEvent.getOrderId());

        // Procesar las notificaciones usando la plantilla `order_finished`
        processOrderEventWithTemplate(orderEvent, "order_finished");
    }

    /**
     * Procesa un evento de notificación utilizando una plantilla específica.
     *
     * @param orderEvent   Evento recibido del sistema que describe el estado de la orden.
     * @param templateName Nombre de la plantilla de correo a utilizar.
     */
    private void processOrderEventWithTemplate(OrderEvent orderEvent, String templateName) {

        // Obtener la plantilla de correo según el nombre del estado
        EmailTemplate template = emailTemplateRepository.findByName(templateName);

        if (template == null) {
            log.warn("La plantilla '{}' no se encontró. No se enviará ninguna notificación para la orden ID: {}", templateName, orderEvent.getOrderId());
            sendConfirmation(orderEvent, "FAILED");
            return;
        }

        // Validar destinatarios
        if (orderEvent.getRecipientEmails() == null || orderEvent.getRecipientEmails().isEmpty()) {
            log.warn("Eventos sin destinatarios para la orden ID: {}", orderEvent.getOrderId());
            saveNotification(orderEvent.getOrderId(), null, NotificationStatus.FAILED, "Sin destinatarios", template);
            sendConfirmation(orderEvent, "FAILED");
            return;
        }

        // Enviar notificaciones a cada destinatario
        orderEvent.getRecipientEmails().forEach(email -> sendNotification(template, orderEvent, email));

        // Enviar confirmación de éxito
        sendConfirmation(orderEvent, "SUCCESS");
    }

    /**
     * Envía una notificación de correo a un destinatario específico y guarda el estado.
     *
     * @param template       Plantilla utilizada para construir el correo.
     * @param orderEvent     Evento recibido que describe la orden y otros detalles.
     * @param recipientEmail Email del destinatario.
     */
    private void sendNotification(EmailTemplate template, OrderEvent orderEvent, String recipientEmail) {

        UUID orderId = orderEvent.getOrderId();
        log.info("Enviando notificación a: {} para la orden ID: {}", recipientEmail, orderId);

        // Construir el contenido del correo usando la plantilla
        String subject = template.getSubject();
        String content = template.getBody()
                .replace("{orderId}", orderId.toString())
                .replace("{recipientEmail}", recipientEmail);

        try {

            // Enviar el correo con SendGrid
            sendGridEmailService.sendEmail(recipientEmail, subject, content);

            // Persistir notificación
            saveNotification(orderEvent.getOrderId(), recipientEmail, NotificationStatus.SENT, "Correo enviado exitosamente.", template);

        } catch (Exception ex) {
            log.error("Fallo al enviar correo para ID de orden: {}, destinatario: {}", orderEvent.getOrderId(), recipientEmail, ex);
            saveNotification(orderEvent.getOrderId(), recipientEmail, NotificationStatus.FAILED, ex.getMessage(), template);
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
     * Guarda el resultado de una notificación en la base de datos.
     *
     * @param orderId ID de la orden asociada.
     * @param recipientEmail Correo electrónico del destinatario.
     * @param status Estado de la notificación.
     * @param message Mensaje adicional relacionado con el envío.
     */
    public void saveNotification(UUID orderId, String recipientEmail, NotificationStatus status, String message, EmailTemplate template) {

        Notification notification = Notification.builder()
                .orderId(orderId)
                .recipientEmail(recipientEmail)
                .status(status)
                .message(message)
                .template(template)
                .build();

        notificationRepository.save(notification);
        log.info("Notificación registrada: Orden ID: {}, Email: {}, Estado: {}", orderId, recipientEmail, status);
    }
}
