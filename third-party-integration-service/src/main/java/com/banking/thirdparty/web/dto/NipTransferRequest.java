package com.banking.thirdparty.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record NipTransferRequest(
        @NotBlank String sourceAccount,
        @NotBlank String destBank,
        @NotBlank String destAccount,
        @NotNull @Positive BigDecimal amount
) {
}
