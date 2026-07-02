package com.banking.core.service;

import com.banking.core.domain.Account;
import com.banking.core.domain.Direction;
import com.banking.core.domain.LedgerEntry;
import com.banking.core.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * "Journal Entry Engine" stored-procedure equivalent.
 *
 * <p>Enforces double-entry (every posting has an equal DR and CR), updates the
 * running balances of both accounts and writes the ledger entries that make up the
 * general ledger. Callers must have already loaded both accounts under a write lock.</p>
 */
@Service
public class JournalEntryEngine {

    private final LedgerEntryRepository ledgerRepository;

    public JournalEntryEngine(LedgerEntryRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * Post a balanced DR/CR pair for {@code amount} moving from {@code debit} to {@code credit}.
     * Balances are updated in memory (the surrounding transaction flushes them) and two
     * immutable ledger entries are persisted.
     */
    public void post(String transactionId, Account debit, Account credit, BigDecimal amount, String narration) {
        BigDecimal debitBalance = debit.getBalance().subtract(amount);
        BigDecimal creditBalance = credit.getBalance().add(amount);

        debit.setBalance(debitBalance);
        credit.setBalance(creditBalance);

        ledgerRepository.save(new LedgerEntry(transactionId, debit.getAccountNumber(), Direction.DR, amount, debitBalance, narration));
        ledgerRepository.save(new LedgerEntry(transactionId, credit.getAccountNumber(), Direction.CR, amount, creditBalance, narration));
    }
}
