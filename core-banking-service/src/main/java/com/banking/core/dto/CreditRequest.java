package com.banking.core.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreditRequest(
        @NotBlank String accountNumber,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String narration
) {
}
