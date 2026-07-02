package com.banking.thirdparty.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record KycRequest(
        @NotBlank @Pattern(regexp = "\\d{11}", message = "bvn must be 11 digits") String bvn,
        @NotBlank String firstName,
        @NotBlank String lastName
) {
}
