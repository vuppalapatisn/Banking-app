package com.banking.thirdparty.web.dto;

public record NipTransferResponse(
        String status,
        String sessionId
) {
}
