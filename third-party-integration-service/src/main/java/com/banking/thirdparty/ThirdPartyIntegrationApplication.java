package com.banking.thirdparty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Third Party Integration Service. Exposes simulated adapters
 * for external systems (payment switch, KYC/BVN/NIN verification, inter-bank NIP
 * transfers and a bank directory). No real network calls are made.
 */
@SpringBootApplication
public class ThirdPartyIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThirdPartyIntegrationApplication.class, args);
    }
}
