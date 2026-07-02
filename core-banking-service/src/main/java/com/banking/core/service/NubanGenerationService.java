package com.banking.core.service;

import com.banking.core.repository.AccountRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * "NUBAN Generation" stored-procedure equivalent.
 *
 * <p>Generates a unique 10-digit account number: a 9-digit serial plus a check digit
 * computed with the Central Bank of Nigeria (CBN) NUBAN algorithm, then verifies
 * format and uniqueness before it is assigned to a customer.</p>
 */
@Service
public class NubanGenerationService {

    private static final int[] WEIGHTS = {3, 7, 3, 3, 7, 3, 3, 7, 3, 3, 7, 3};

    private final AccountRepository accountRepository;
    private final String bankCode;
    private final AtomicLong serialCounter = new AtomicLong(100_000_000L);

    public NubanGenerationService(AccountRepository accountRepository,
                                  @Value("${banking.nuban.bank-code:090}") String bankCode) {
        this.accountRepository = accountRepository;
        this.bankCode = bankCode;
    }

    @PostConstruct
    void seedCounter() {
        // Continue serials past whatever already exists so numbers stay unique across restarts.
        serialCounter.set(100_000_000L + accountRepository.count());
    }

    /**
     * Generate the next unique, format-valid NUBAN.
     */
    public String generate() {
        for (int attempt = 0; attempt < 1_000; attempt++) {
            long serial = serialCounter.incrementAndGet();
            String serial9 = String.format("%09d", serial % 1_000_000_000L);
            String nuban = serial9 + checkDigit(serial9);
            if (!accountRepository.existsByAccountNumber(nuban)) {
                return nuban;
            }
        }
        throw new IllegalStateException("Unable to allocate a unique NUBAN");
    }

    /**
     * CBN NUBAN check-digit: weighted sum of (3-digit bank code + 9-digit serial),
     * mod 10, subtracted from 10 (10 collapses to 0).
     */
    int checkDigit(String serial9) {
        String seed = String.format("%03d", Integer.parseInt(bankCode) % 1000) + serial9;
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += Character.digit(seed.charAt(i), 10) * WEIGHTS[i];
        }
        int check = 10 - (sum % 10);
        return check == 10 ? 0 : check;
    }
}
