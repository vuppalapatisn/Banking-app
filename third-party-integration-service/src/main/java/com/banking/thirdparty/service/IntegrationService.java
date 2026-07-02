package com.banking.thirdparty.service;

import com.banking.thirdparty.web.dto.BankInfo;
import com.banking.thirdparty.web.dto.KycRequest;
import com.banking.thirdparty.web.dto.KycResponse;
import com.banking.thirdparty.web.dto.NipTransferRequest;
import com.banking.thirdparty.web.dto.NipTransferResponse;
import com.banking.thirdparty.web.dto.PaymentSwitchRequest;
import com.banking.thirdparty.web.dto.PaymentSwitchResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Produces simulated responses for the external systems this service fronts. No
 * real network calls are made; values are derived deterministically from a UUID so
 * the shape and behaviour match a real integration while remaining reproducible.
 */
@Service
public class IntegrationService {

    private static final double FIXED_MATCH_SCORE = 0.97;

    private static final List<BankInfo> BANKS = List.of(
            new BankInfo("011", "First Bank"),
            new BankInfo("058", "GTBank"),
            new BankInfo("057", "Zenith Bank"),
            new BankInfo("044", "Access Bank"),
            new BankInfo("232", "Sterling Bank")
    );

    public KycResponse verifyKyc(KycRequest request) {
        String reference = "KYC-" + shortId();
        return new KycResponse(true, reference, FIXED_MATCH_SCORE);
    }

    public PaymentSwitchResponse routePayment(PaymentSwitchRequest request) {
        String authCode = shortId().substring(0, 6).toUpperCase();
        String rrn = digits(shortId(), 12);
        return new PaymentSwitchResponse(true, authCode, rrn);
    }

    public NipTransferResponse transfer(NipTransferRequest request) {
        String sessionId = digits(shortId(), 30);
        return new NipTransferResponse("SUCCESS", sessionId);
    }

    public List<BankInfo> banks() {
        return BANKS;
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** Derives a numeric string of the requested length from a hex seed. */
    private String digits(String seed, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; sb.length() < length; i++) {
            char c = seed.charAt(i % seed.length());
            sb.append(Character.digit(c, 16) % 10);
        }
        return sb.toString();
    }
}
