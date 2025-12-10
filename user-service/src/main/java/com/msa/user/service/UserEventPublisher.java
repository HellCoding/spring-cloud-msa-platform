package com.msa.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Publishes domain events to RabbitMQ for asynchronous cross-service communication.
 *
 * Using a topic exchange so consumers can subscribe to specific event patterns
 * (e.g., "user.profile.*" catches both updates and deletes). This decouples
 * the user-service from knowing which services consume its events,
 * following the publish-subscribe pattern.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    private static final String EXCHANGE = "user.events";
    private static final String ROUTING_KEY = "user.profile.updated";

    private final RabbitTemplate rabbitTemplate;

    public void publishProfileUpdated(Long userId, String nickname) {
        Map<String, Object> event = Map.of(
                "userId", userId,
                "nickname", nickname,
                "eventType", "PROFILE_UPDATED",
                "timestamp", LocalDateTime.now().toString()
        );

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
        log.info("Published profile update event for user: {}", userId);
    }
}
