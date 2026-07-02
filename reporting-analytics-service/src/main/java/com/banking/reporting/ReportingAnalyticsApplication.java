package com.banking.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Reporting and Analytics Service. Consumes committed
 * transaction events off the Kafka event bus, maintains daily aggregates and
 * exposes REST endpoints for daily reports, summaries and per-account reporting.
 */
@SpringBootApplication
public class ReportingAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportingAnalyticsApplication.class, args);
    }
}
