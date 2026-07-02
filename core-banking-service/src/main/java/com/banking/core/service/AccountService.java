package com.banking.core.service;

import com.banking.common.event.AccountEvent;
import com.banking.core.domain.Account;
import com.banking.core.domain.AccountType;
import com.banking.core.domain.Customer;
import com.banking.core.dto.BalanceResponse;
import com.banking.core.dto.OpenAccountRequest;
import com.banking.core.repository.AccountRepository;
import com.banking.core.repository.CustomerRepository;
import com.banking.core.exception.AccountNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Account onboarding: validates input, generates a NUBAN, persists the customer and
 * account atomically and emits an {@link AccountEvent} to the integration layer.
 */
@Service
public class AccountService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final NubanGenerationService nubanGenerationService;
    private final EventPublisher eventPublisher;

    public AccountService(CustomerRepository customerRepository,
                          AccountRepository accountRepository,
                          NubanGenerationService nubanGenerationService,
                          EventPublisher eventPublisher) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.nubanGenerationService = nubanGenerationService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Account openAccount(OpenAccountRequest request) {
        AccountType type = parseType(request.accountType());
        String currency = (request.currency() == null || request.currency().isBlank()) ? "NGN" : request.currency();

        String customerId = "CUS" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Customer customer = customerRepository.save(new Customer(
                customerId, request.firstName(), request.lastName(),
                request.email(), request.phone(), request.bvn()));

        String nuban = nubanGenerationService.generate();
        String accountName = customer.getFirstName() + " " + customer.getLastName();
        Account account = accountRepository.save(new Account(nuban, customerId, accountName, type, currency));

        eventPublisher.publish(new AccountEvent(
                UUID.randomUUID().toString(), nuban, customerId,
                AccountEvent.AccountEventType.CREATED, Instant.now()));

        return account;
    }

    @Transactional(readOnly = true)
    public Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountNumber) {
        Account account = getAccount(accountNumber);
        return new BalanceResponse(account.getAccountNumber(), account.getBalance(), account.getCurrency(), Instant.now());
    }

    private AccountType parseType(String raw) {
        AccountType type;
        try {
            type = AccountType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid account type: " + raw + " (expected SAVINGS or CURRENT)");
        }
        if (type == AccountType.GL) {
            throw new IllegalArgumentException("GL accounts cannot be opened via the customer API");
        }
        return type;
    }
}
