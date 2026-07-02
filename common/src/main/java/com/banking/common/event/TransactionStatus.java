package com.banking.common.event;

/**
 * Lifecycle status of a core-banking transaction.
 */
public enum TransactionStatus {
    PENDING,
    POSTED,
    FAILED,
    REVERSED
}
