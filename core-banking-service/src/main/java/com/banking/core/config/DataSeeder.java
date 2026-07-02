package com.banking.core.config;

import com.banking.core.domain.Account;
import com.banking.core.domain.AccountType;
import com.banking.core.repository.AccountRepository;
import com.banking.core.service.GeneralLedger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Ensures the internal General-Ledger cash account exists so that customer deposits
 * and withdrawals always have a balanced contra side.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AccountRepository accountRepository;

    public DataSeeder(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(String... args) {
        if (accountRepository.existsByAccountNumber(GeneralLedger.CASH)) {
            return;
        }
        Account cash = new Account(GeneralLedger.CASH, "BANK", "BANK CASH GL", AccountType.GL, "NGN");
        cash.setBalance(new BigDecimal("1000000000000")); // funding float for the demo vault
        accountRepository.save(cash);
        log.info("Seeded General-Ledger cash account {}", GeneralLedger.CASH);
    }
}
