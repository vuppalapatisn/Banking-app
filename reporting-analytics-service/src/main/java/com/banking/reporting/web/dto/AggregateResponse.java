package com.banking.reporting.web.dto;

import com.banking.common.event.TransactionType;
import com.banking.reporting.domain.DailyAggregate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Row of a daily report: one bucket keyed by date, account and transaction type. */
public record AggregateResponse(
        LocalDate date,
        String account,
        TransactionType type,
        long transactionCount,
        BigDecimal totalAmount
) {
    public static AggregateResponse from(DailyAggregate aggregate) {
        return new AggregateResponse(
                aggregate.getReportDate(),
                aggregate.getAccount(),
                aggregate.getType(),
                aggregate.getTransactionCount(),
                aggregate.getTotalAmount());
    }
}
