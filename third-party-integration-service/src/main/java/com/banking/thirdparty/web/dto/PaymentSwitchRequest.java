package com.banking.thirdparty.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentSwitchRequest(
        @NotBlank @Pattern(regexp = "\\d{12,19}", message = "cardPan must be 12-19 digits") String cardPan,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency
) {
}
