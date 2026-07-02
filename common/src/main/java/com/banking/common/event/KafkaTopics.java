package com.banking.common.event;

/**
 * Central registry of the Kafka topics that make up the event bus / message broker
 * in the core-banking architecture. Kept in the shared module so producers and
 * consumers can never drift apart on topic names.
 */
public final class KafkaTopics {

    private KafkaTopics() {
    }

    /** Committed ledger transactions (credit / debit / transfer). */
    public static final String TRANSACTIONS = "banking.transactions";

    /** Account lifecycle events (created / updated / closed). */
    public static final String ACCOUNTS = "banking.accounts";
}
