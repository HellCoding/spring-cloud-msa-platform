package com.msa.notification.consumer;

import com.msa.notification.config.RabbitMQConfig;
import com.msa.notification.domain.NotificationRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumes user-related domain events from RabbitMQ.
 *
 * The @RabbitListener annotation handles connection management, message
 * deserialization, and acknowledgment automatically. If processing fails,
 * Spring AMQP will nack the message and requeue it (configurable).
 *
 * In production, this consumer would:
 * - Send push notifications via Firebase/APNs
 * - Send transactional emails via SendGrid/SES
 * - Persist notification history to a database
 * - Implement idempotency to handle redelivered messages
 */
@Slf4j
@Component
public class UserEventConsumer {

    private final ConcurrentHashMap<String, NotificationRecord> notificationHistory =
            new ConcurrentHashMap<>();

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleUserProfileEvent(Map<String, Object> event) {
        log.info("Received user event: {}", event);

        String eventType = String.valueOf(event.get("eventType"));
        Object userIdObj = event.get("userId");
        Long userId = userIdObj instanceof Integer
                ? ((Integer) userIdObj).longValue()
                : Long.valueOf(String.valueOf(userIdObj));
        String nickname = String.valueOf(event.getOrDefault("nickname", ""));

        String message = String.format("User %d updated profile. New nickname: %s", userId, nickname);

        NotificationRecord record = new NotificationRecord(
                UUID.randomUUID().toString(),
                userId,
                eventType,
                message,
                LocalDateTime.now()
        );

        // Store in-memory for demonstration. ConcurrentHashMap ensures
        // thread safety since RabbitMQ listeners run on a thread pool.
        notificationHistory.put(record.id(), record);

        log.info("Notification created: {} | Total history size: {}",
                record.message(), notificationHistory.size());
    }

    public Map<String, NotificationRecord> getNotificationHistory() {
        return Map.copyOf(notificationHistory);
    }
}
