package com.banking.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Core-banking data exposed to LLM clients as Model Context Protocol (MCP) tools.
 *
 * <p>Each method is annotated with {@link Tool} so Spring AI can advertise it to a
 * connected MCP client (e.g. Claude Desktop). Data is SIMULATED in-memory so the
 * server runs with no external dependencies, but every tool first attempts to call
 * the real core-banking service at {@code ${CORE_BANKING_URI}} inside a try/catch and
 * falls back to the simulated response on any error.
 */
@Component
public class BankingTools {

    private static final Logger log = LoggerFactory.getLogger(BankingTools.class);

    private final RestClient coreBanking;

    public BankingTools(@Value("${core-banking.uri:http://localhost:8081}") String coreBankingUri) {
        this.coreBanking = RestClient.builder().baseUrl(coreBankingUri).build();
    }

    @Tool(description = "Get the current balance for a bank account by its NUBAN account number")
    public String getAccountBalance(
            @ToolParam(description = "10-digit NUBAN account number") String accountNumber) {
        try {
            String remote = coreBanking.get()
                    .uri("/api/accounts/{acct}/balance", accountNumber)
                    .retrieve()
                    .body(String.class);
            if (remote != null && !remote.isBlank()) {
                return remote;
            }
        } catch (Exception ex) {
            log.debug("core-banking unavailable, using simulated balance for {}: {}",
                    accountNumber, ex.getMessage());
        }
        BigDecimal balance = simulatedBalance(accountNumber);
        return "Account %s has an available balance of NGN %s (simulated)."
                .formatted(accountNumber, balance.toPlainString());
    }

    @Tool(description = "List the most recent transactions for an account")
    public String listRecentTransactions(
            @ToolParam(description = "10-digit NUBAN account number") String accountNumber) {
        try {
            String remote = coreBanking.get()
                    .uri("/api/accounts/{acct}/transactions", accountNumber)
                    .retrieve()
                    .body(String.class);
            if (remote != null && !remote.isBlank()) {
                return remote;
            }
        } catch (Exception ex) {
            log.debug("core-banking unavailable, using simulated transactions for {}: {}",
                    accountNumber, ex.getMessage());
        }

        StringBuilder sb = new StringBuilder("Recent transactions for account %s (simulated):%n"
                .formatted(accountNumber));
        String[] narrations = {"POS Purchase", "Salary Credit", "ATM Withdrawal", "Transfer Out", "Airtime"};
        Instant now = Instant.now();
        for (int i = 0; i < narrations.length; i++) {
            BigDecimal amount = BigDecimal.valueOf(
                    ThreadLocalRandom.current().nextInt(1_000, 500_000));
            String type = (i % 2 == 0) ? "DEBIT" : "CREDIT";
            sb.append("  - %s | %s | NGN %s | %s%n".formatted(
                    now.minus(i, ChronoUnit.DAYS),
                    type,
                    amount.toPlainString(),
                    narrations[i]));
        }
        return sb.toString();
    }

    @Tool(description = "Get the bank's product catalog with interest rates and fees")
    public String getProductCatalog() {
        try {
            String remote = coreBanking.get()
                    .uri("/api/products")
                    .retrieve()
                    .body(String.class);
            if (remote != null && !remote.isBlank()) {
                return remote;
            }
        } catch (Exception ex) {
            log.debug("core-banking unavailable, using simulated product catalog: {}", ex.getMessage());
        }

        Map<String, String> products = new LinkedHashMap<>();
        products.put("Savings Account", "Interest 4.0% p.a. | Maintenance fee NGN 0 | Min balance NGN 1,000");
        products.put("Current Account", "Interest 0.0% p.a. | Maintenance fee NGN 100/month | Min balance NGN 5,000");
        products.put("Fixed Deposit (90d)", "Interest 12.5% p.a. | Early-withdrawal penalty 2.0% | Min NGN 100,000");
        products.put("Personal Loan", "APR 22.0% | Processing fee 1.5% | Tenor up to 36 months");

        StringBuilder sb = new StringBuilder("Bank product catalog (simulated):%n".formatted());
        products.forEach((name, terms) -> sb.append("  - %s: %s%n".formatted(name, terms)));
        return sb.toString();
    }

    /** Deterministic pseudo-balance derived from the account number so repeat calls are stable. */
    private BigDecimal simulatedBalance(String accountNumber) {
        long seed = accountNumber == null ? 0 : Math.abs(accountNumber.hashCode());
        return BigDecimal.valueOf(50_000 + (seed % 1_000_000)).movePointLeft(0);
    }
}
