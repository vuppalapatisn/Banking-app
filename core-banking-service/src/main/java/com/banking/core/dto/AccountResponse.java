package com.banking.core.dto;

import com.banking.core.domain.Account;

import java.math.BigDecimal;

public record AccountResponse(
        String accountNumber,
        String customerId,
        String accountName,
        String type,
        String currency,
        BigDecimal balance,
        String status
) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(
                a.getAccountNumber(),
                a.getCustomerId(),
                a.getAccountName(),
                a.getType().name(),
                a.getCurrency(),
                a.getBalance(),
                a.getStatus().name()
        );
    }
}
