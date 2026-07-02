package com.banking.reporting.web.dto;

import com.banking.common.event.TransactionType;

import java.math.BigDecimal;

/** Aggregated totals across all recorded activity, grouped by transaction type. */
public record SummaryResponse(
        TransactionType type,
        long transactionCount,
        BigDecimal totalAmount
) {
}
