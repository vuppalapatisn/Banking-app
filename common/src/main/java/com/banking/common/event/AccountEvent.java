package com.banking.common.event;

import java.time.Instant;

/**
 * Published on {@link KafkaTopics#ACCOUNTS} when an account is created, updated or closed.
 */
public record AccountEvent(
        String eventId,
        String accountNumber,
        String customerId,
        AccountEventType eventType,
        Instant timestamp
) {
    public enum AccountEventType {
        CREATED,
        UPDATED,
        CLOSED
    }
}
