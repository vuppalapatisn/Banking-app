package com.banking.product.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Payload used to create or update a {@link com.banking.product.domain.Product}.
 */
public record ProductRequest(
        @NotBlank String code,
        @NotBlank String name,
        String type,
        BigDecimal interestRate,
        BigDecimal monthlyFee,
        BigDecimal dailyDebitLimit,
        BigDecimal minBalance,
        boolean active) {
}
