package com.banking.thirdparty.web.dto;

public record PaymentSwitchResponse(
        boolean approved,
        String authCode,
        String rrn
) {
}
