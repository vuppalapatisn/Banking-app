package com.banking.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Core Banking service — the highly transactional (ACID) heart of the platform.
 * Everything that touches money lives here: credit, debit, fund transfer,
 * NUBAN generation, real-time balance update and the double-entry journal engine.
 */
@SpringBootApplication
public class CoreBankingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreBankingApplication.class, args);
    }
}
