package com.msa.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Notification Service - consumes domain events and dispatches notifications.
 *
 * This service demonstrates the event-driven consumer side of the MSA platform.
 * It subscribes to RabbitMQ topics and reacts to events from other services
 * without those services needing to know about notification logic.
 * In production, this would integrate with email, SMS, or push notification providers.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
