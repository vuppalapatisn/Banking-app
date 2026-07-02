package com.banking.core.service;

/**
 * Well-known internal General-Ledger account numbers used as the contra side of
 * customer deposits and withdrawals so every transaction stays double-entry balanced.
 */
public final class GeneralLedger {

    private GeneralLedger() {
    }

    /** Bank cash / vault contra account. */
    public static final String CASH = "0000000000";
}
