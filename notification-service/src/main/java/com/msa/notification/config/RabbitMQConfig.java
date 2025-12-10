package com.msa.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ consumer-side configuration.
 *
 * Declares the queue, exchange, and binding on the consumer side.
 * RabbitMQ will auto-create these resources if they don't exist yet.
 * The routing key "user.profile.*" uses a wildcard to capture all
 * profile-related events (updated, deleted, etc.) in a single queue.
 */
@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "notification.user.profile";
    public static final String EXCHANGE_NAME = "user.events";
    public static final String ROUTING_KEY = "user.profile.*";

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue notificationQueue() {
        // Durable queue survives broker restarts, preventing message loss
        // in production when the broker is restarted for maintenance.
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Binding binding(Queue notificationQueue, TopicExchange userEventsExchange) {
        return BindingBuilder
                .bind(notificationQueue)
                .to(userEventsExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
