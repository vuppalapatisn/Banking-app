package com.banking.aimonitor.service;

import com.banking.aimonitor.domain.Anomaly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Produces a natural-language risk summary for an account's anomalies.
 *
 * <p>The Anthropic {@link AnthropicChatModel} is injected via {@link ObjectProvider} so it
 * is optional — the service starts even when the Anthropic auto-configuration is not fully
 * usable. A {@link ChatClient} is built lazily only when the model is present AND a real
 * API key was supplied. When Claude is unavailable (no key, missing model, or any error)
 * the service falls back to a deterministic heuristic summary.
 */
@Service
public class RiskAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(RiskAnalysisService.class);

    private final ObjectProvider<AnthropicChatModel> chatModelProvider;
    private final String apiKey;

    private volatile ChatClient chatClient;

    public RiskAnalysisService(
            ObjectProvider<AnthropicChatModel> chatModelProvider,
            @Value("${spring.ai.anthropic.api-key:dummy-key}") String apiKey) {
        this.chatModelProvider = chatModelProvider;
        this.apiKey = apiKey;
    }

    /** True when a real (non-dummy) API key is configured. */
    private boolean hasRealKey() {
        return apiKey != null && !apiKey.isBlank() && !"dummy-key".equals(apiKey);
    }

    private ChatClient chatClientOrNull() {
        if (!hasRealKey()) {
            return null;
        }
        if (chatClient == null) {
            AnthropicChatModel model = chatModelProvider.getIfAvailable();
            if (model != null) {
                chatClient = ChatClient.builder(model).build();
            }
        }
        return chatClient;
    }

    /**
     * Summarise the risk posed by an account's anomalies. Uses Claude when available,
     * otherwise a deterministic heuristic.
     */
    public String summarise(String accountNumber, List<Anomaly> anomalies) {
        ChatClient client = chatClientOrNull();
        if (client != null) {
            try {
                String prompt = buildPrompt(accountNumber, anomalies);
                String response = client.prompt()
                        .user(prompt)
                        .call()
                        .content();
                if (response != null && !response.isBlank()) {
                    return response;
                }
            } catch (Exception ex) {
                log.warn("Claude risk summary failed, falling back to heuristic: {}", ex.getMessage());
            }
        }
        return heuristicSummary(accountNumber, anomalies);
    }

    private String buildPrompt(String accountNumber, List<Anomaly> anomalies) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a bank fraud-risk analyst. Summarise the risk for account ")
                .append(accountNumber)
                .append(" based on the following detected anomalies. ")
                .append("Give a short risk level (LOW/MEDIUM/HIGH) and a one-paragraph explanation.\n\n");
        if (anomalies.isEmpty()) {
            sb.append("No anomalies were detected for this account.");
        } else {
            for (Anomaly a : anomalies) {
                sb.append("- [").append(a.rule()).append("] amount=").append(a.amount())
                        .append(" at ").append(a.detectedAt())
                        .append(": ").append(a.explanation()).append('\n');
            }
        }
        return sb.toString();
    }

    /** Deterministic, LLM-free summary used when Claude is unavailable. */
    String heuristicSummary(String accountNumber, List<Anomaly> anomalies) {
        if (anomalies.isEmpty()) {
            return "Account %s: LOW risk. No anomalies detected.".formatted(accountNumber);
        }
        Map<String, Long> byRule = anomalies.stream()
                .collect(java.util.stream.Collectors.groupingBy(Anomaly::rule, java.util.stream.Collectors.counting()));
        long large = byRule.getOrDefault("LARGE_AMOUNT", 0L);
        long velocity = byRule.getOrDefault("VELOCITY", 0L);

        String level;
        if (large > 0 && velocity > 0) {
            level = "HIGH";
        } else if (large > 0 || velocity > 2) {
            level = "MEDIUM";
        } else {
            level = "LOW";
        }

        return ("Account %s: %s risk based on %d anomaly(ies) — %d LARGE_AMOUNT, %d VELOCITY. "
                + "Recommend review of the flagged transactions. "
                + "(Heuristic summary; set ANTHROPIC_API_KEY to enable Claude-generated analysis.)")
                .formatted(accountNumber, level, anomalies.size(), large, velocity);
    }
}
