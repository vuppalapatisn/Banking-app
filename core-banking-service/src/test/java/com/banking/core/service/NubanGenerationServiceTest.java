package com.banking.core.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NubanGenerationServiceTest {

    // Repository is not needed for the pure check-digit calculation.
    private final NubanGenerationService service = new NubanGenerationService(null, "090");

    @Test
    void computesCbnCheckDigit() {
        // seed = "090" + "000000001", weighted sum = 66, 10 - (66 % 10) = 4
        assertThat(service.checkDigit("000000001")).isEqualTo(4);
    }

    @Test
    void checkDigitIsAlwaysASingleDigit() {
        for (int i = 1; i < 500; i++) {
            int digit = service.checkDigit(String.format("%09d", i));
            assertThat(digit).isBetween(0, 9);
        }
    }
}
