package com.banking.core.dto;

import com.banking.core.domain.TransactionRecord;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        String transactionId,
        String type,
        String status,
        String debitAccount,
        String creditAccount,
        BigDecimal amount,
        String currency,
        String narration,
        Instant createdAt
) {
    public static TransactionResponse from(TransactionRecord t) {
        return new TransactionResponse(
                t.getTransactionId(),
                t.getType().name(),
                t.getStatus().name(),
                t.getDebitAccount(),
                t.getCreditAccount(),
                t.getAmount(),
                t.getCurrency(),
                t.getNarration(),
                t.getCreatedAt()
        );
    }
}
