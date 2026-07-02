package com.banking.core.domain;

public enum AccountType {
    SAVINGS,
    CURRENT,
    /** Internal General-Ledger account (e.g. the bank cash contra account). */
    GL
}
