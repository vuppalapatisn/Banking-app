package com.banking.common.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published on {@link KafkaTopics#TRANSACTIONS} by the core-banking service after a
 * transaction is committed to the ledger. Consumed by the notification, reporting,
 * audit and AI-monitor services (the asynchronous / event-driven integration layer).
 */
public record TransactionEvent(
        String eventId,
        String transactionId,
        TransactionType type,
        TransactionStatus status,
        String debitAccount,
        String creditAccount,
        BigDecimal amount,
        String currency,
        BigDecimal debitBalanceAfter,
        BigDecimal creditBalanceAfter,
        String narration,
        Instant timestamp
) {
}
