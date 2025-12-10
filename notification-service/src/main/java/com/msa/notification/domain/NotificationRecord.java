package com.msa.notification.domain;

import java.time.LocalDateTime;

/**
 * Lightweight notification record stored in-memory.
 *
 * Using a Java record for immutability. In production, this would be
 * a JPA entity persisted to a database for audit trail and retry logic.
 */
public record NotificationRecord(
        String id,
        Long userId,
        String type,
        String message,
        LocalDateTime createdAt
) {}
