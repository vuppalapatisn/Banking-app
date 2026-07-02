package com.banking.thirdparty.web.dto;

public record KycResponse(
        boolean verified,
        String reference,
        double matchScore
) {
}
