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
    public static final String ROUTING_KEY = "order.created";
    public static final String NOTIFICATIONS_EXCHANGE = "notifications.exchange";

    /**
     * Configura la cola durable 'notifications.queue'.
     */
    @Bean
    public Queue notificationsQueue() {
        log.info("Creando cola: " + QUEUE_NAME);
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    /**
     * Configura el exchange 'orders.exchange' de tipo Topic.
     */
    @Bean
    public TopicExchange ordersExchange() {
        log.info("Creando exchange: " + EXCHANGE_NAME);
        return new TopicExchange(EXCHANGE_NAME);
    }

    /**
     * Vincula la cola 'notifications.queue' al exchange 'orders.exchange' con una routing key.
     */
    @Bean
    public Binding binding(Queue notificationsQueue, TopicExchange ordersExchange) {
        log.info("Creando binding entre exchange '" + EXCHANGE_NAME + "' y cola '" + QUEUE_NAME
                + "' con routing key '" + ROUTING_KEY + "'");
        return BindingBuilder.bind(notificationsQueue)
                .to(ordersExchange)
                .with(ROUTING_KEY);
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
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    /**
     * Configuración del exchange para confirmaciones.
     *
     * @return un intercambio (Topic Exchange) para confirmaciones de notificaciones.
     */
    @Bean
    public TopicExchange notificationsExchange() {
        return new TopicExchange(NOTIFICATIONS_EXCHANGE);
    }
}