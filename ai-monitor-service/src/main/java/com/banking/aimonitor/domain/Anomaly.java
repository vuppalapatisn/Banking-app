package com.banking.aimonitor.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A detected anomaly on the transaction stream. Stored in-memory (no database).
 */
public record Anomaly(
        String id,
        String accountNumber,
        String rule,
        BigDecimal amount,
        Instant detectedAt,
        String explanation
) {
}
