package com.infragest.infra_notifications_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "orders.exchange";
    public static final String QUEUE_NAME = "notifications.queue";
    public static final String ROUTING_KEY_PATTERN = "order.state.*";
    public static final String NOTIFICATIONS_EXCHANGE = "notifications.exchange";

    /**
     * Configura la cola durable 'notifications.queue' para escuchar eventos de órdenes.
     */
    @Bean
    public Queue notificationsQueue() {
        log.info("Creando cola: " + QUEUE_NAME);
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    /**
     * Configura el exchange 'orders.exchange' de tipo Topic.
     * Este es el exchange donde el microservicio de órdenes publica los cambios.
     */
    @Bean
    public TopicExchange ordersExchange() {
        log.info("Creando exchange: " + EXCHANGE_NAME);
        return new TopicExchange(EXCHANGE_NAME);
    }

    /**
     * Vincula la cola 'notifications.queue' al exchange 'orders.exchange' usando un patrón de routing key.
     * Este patrón permite escuchar todos los eventos relacionados con los estados de las órdenes (`order.state.*`).
     */
    @Bean
    public Binding binding(Queue notificationsQueue, TopicExchange ordersExchange) {
        log.info("Creando binding entre exchange '" + EXCHANGE_NAME + "' y cola '" + QUEUE_NAME
                + "' con patrón de routing key '" + ROUTING_KEY_PATTERN + "'");
        return BindingBuilder.bind(notificationsQueue)
                .to(ordersExchange)
                .with(ROUTING_KEY_PATTERN);
    }

    /**
     * Configura un convertidor de mensajes para usar JSON en lugar de Serializable.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        log.info("Configurando Jackson2JsonMessageConverter para JSON");
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Asocia el convertidor JSON al RabbitTemplate.
     * Este objeto se utilizará para enviar mensajes como JSON.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    /**
     * Configuración del exchange para confirmaciones de notificaciones.
     * Este es un exchange separado para usar en confirmaciones, si es necesario.
     *
     * @return un Topic Exchange de confirmación.
     */
    @Bean
    public TopicExchange notificationsExchange() {
        log.info("Creando exchange de confirmaciones: " + NOTIFICATIONS_EXCHANGE);
        return new TopicExchange(NOTIFICATIONS_EXCHANGE);
    }
}