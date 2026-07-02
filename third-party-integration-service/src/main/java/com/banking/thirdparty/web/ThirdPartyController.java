package com.banking.thirdparty.web;

import com.banking.thirdparty.service.IntegrationService;
import com.banking.thirdparty.web.dto.BankInfo;
import com.banking.thirdparty.web.dto.KycRequest;
import com.banking.thirdparty.web.dto.KycResponse;
import com.banking.thirdparty.web.dto.NipTransferRequest;
import com.banking.thirdparty.web.dto.NipTransferResponse;
import com.banking.thirdparty.web.dto.PaymentSwitchRequest;
import com.banking.thirdparty.web.dto.PaymentSwitchResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Simulated third-party integration endpoints: KYC/BVN verification, payment-switch
 * routing, inter-bank NIP transfers and a bank directory.
 */
@RestController
@RequestMapping("/api/third-party")
public class ThirdPartyController {

    private final IntegrationService integrationService;

    public ThirdPartyController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @PostMapping("/kyc/verify")
    public KycResponse verifyKyc(@Valid @RequestBody KycRequest request) {
        return integrationService.verifyKyc(request);
    }

    @PostMapping("/payment-switch/route")
    public PaymentSwitchResponse routePayment(@Valid @RequestBody PaymentSwitchRequest request) {
        return integrationService.routePayment(request);
    }

    @PostMapping("/nip/transfer")
    public NipTransferResponse transfer(@Valid @RequestBody NipTransferRequest request) {
        return integrationService.transfer(request);
    }

    @GetMapping("/banks")
    public List<BankInfo> banks() {
        return integrationService.banks();
    }
}
