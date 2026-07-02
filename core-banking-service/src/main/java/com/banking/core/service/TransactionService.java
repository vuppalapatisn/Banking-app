package com.banking.core.service;

import com.banking.common.event.TransactionEvent;
import com.banking.common.event.TransactionStatus;
import com.banking.common.event.TransactionType;
import com.banking.core.domain.Account;
import com.banking.core.domain.AccountStatus;
import com.banking.core.domain.TransactionRecord;
import com.banking.core.dto.CreditRequest;
import com.banking.core.dto.DebitRequest;
import com.banking.core.dto.TransferRequest;
import com.banking.core.exception.AccountNotFoundException;
import com.banking.core.exception.InsufficientFundsException;
import com.banking.core.exception.InvalidAccountStateException;
import com.banking.core.repository.AccountRepository;
import com.banking.core.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The transactional (ACID) money-movement processes: Credit, Debit and Fund Transfer.
 *
 * <p>Each runs in a single database transaction, loads the affected accounts under a
 * pessimistic write lock (in a deterministic order to avoid deadlocks), validates
 * account state and available balance, posts a balanced journal entry, records the
 * transaction and — after the money has moved — publishes a {@link TransactionEvent}.</p>
 */
@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final JournalEntryEngine journalEntryEngine;
    private final EventPublisher eventPublisher;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              JournalEntryEngine journalEntryEngine,
                              EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.journalEntryEngine = journalEntryEngine;
        this.eventPublisher = eventPublisher;
    }

    /** Credit a customer account (deposit): DR bank cash, CR customer. */
    @Transactional
    public TransactionRecord credit(CreditRequest request) {
        String txnId = newTxnId();
        Locked locked = lockPair(GeneralLedger.CASH, request.accountNumber());
        Account cash = account(locked, GeneralLedger.CASH);
        Account customer = account(locked, request.accountNumber());
        requireActive(customer);

        journalEntryEngine.post(txnId, cash, customer, request.amount(), request.narration());

        TransactionRecord record = save(txnId, TransactionType.CREDIT, GeneralLedger.CASH,
                request.accountNumber(), request.amount(), customer.getCurrency(), request.narration());
        publish(record, cash.getBalance(), customer.getBalance());
        return record;
    }

    /** Debit a customer account (withdrawal): DR customer, CR bank cash. */
    @Transactional
    public TransactionRecord debit(DebitRequest request) {
        String txnId = newTxnId();
        Locked locked = lockPair(request.accountNumber(), GeneralLedger.CASH);
        Account customer = account(locked, request.accountNumber());
        Account cash = account(locked, GeneralLedger.CASH);
        requireActive(customer);
        requireSufficientFunds(customer, request.amount());

        journalEntryEngine.post(txnId, customer, cash, request.amount(), request.narration());

        TransactionRecord record = save(txnId, TransactionType.DEBIT, request.accountNumber(),
                GeneralLedger.CASH, request.amount(), customer.getCurrency(), request.narration());
        publish(record, customer.getBalance(), cash.getBalance());
        return record;
    }

    /** Move funds between two customer accounts: DR source, CR destination. */
    @Transactional
    public TransactionRecord transfer(TransferRequest request) {
        if (request.sourceAccount().equals(request.destinationAccount())) {
            throw new IllegalArgumentException("Source and destination accounts must differ");
        }
        String txnId = newTxnId();
        Locked locked = lockPair(request.sourceAccount(), request.destinationAccount());
        Account source = account(locked, request.sourceAccount());
        Account destination = account(locked, request.destinationAccount());
        requireActive(source);
        requireActive(destination);
        requireSufficientFunds(source, request.amount());

        journalEntryEngine.post(txnId, source, destination, request.amount(), request.narration());

        TransactionRecord record = save(txnId, TransactionType.TRANSFER, request.sourceAccount(),
                request.destinationAccount(), request.amount(), source.getCurrency(), request.narration());
        publish(record, source.getBalance(), destination.getBalance());
        return record;
    }

    @Transactional(readOnly = true)
    public List<TransactionRecord> history(String accountNumber) {
        return transactionRepository
                .findTop50ByDebitAccountOrCreditAccountOrderByCreatedAtDesc(accountNumber, accountNumber);
    }

    // ---------------------------------------------------------------------

    private TransactionRecord save(String txnId, TransactionType type, String debit, String credit,
                                   BigDecimal amount, String currency, String narration) {
        return transactionRepository.save(new TransactionRecord(
                txnId, type, TransactionStatus.POSTED, debit, credit, amount, currency, narration));
    }

    private void publish(TransactionRecord r, BigDecimal debitBalance, BigDecimal creditBalance) {
        eventPublisher.publish(new TransactionEvent(
                UUID.randomUUID().toString(), r.getTransactionId(), r.getType(), r.getStatus(),
                r.getDebitAccount(), r.getCreditAccount(), r.getAmount(), r.getCurrency(),
                debitBalance, creditBalance, r.getNarration(), Instant.now()));
    }

    /** Lock two accounts in a stable order (by account number) to prevent deadlocks. */
    private Locked lockPair(String a, String b) {
        String first = a.compareTo(b) <= 0 ? a : b;
        String second = a.compareTo(b) <= 0 ? b : a;
        Account firstAcct = lock(first);
        Account secondAcct = lock(second);
        return new Locked(firstAcct, secondAcct);
    }

    private Account lock(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private Account account(Locked locked, String accountNumber) {
        if (locked.first().getAccountNumber().equals(accountNumber)) {
            return locked.first();
        }
        return locked.second();
    }

    private void requireActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountStateException("Account " + account.getAccountNumber() + " is " + account.getStatus());
        }
    }

    private void requireSufficientFunds(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(account.getAccountNumber());
        }
    }

    private String newTxnId() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /** Holds the two accounts locked for a transaction, in stable order. */
    private record Locked(Account first, Account second) {
    }
}
