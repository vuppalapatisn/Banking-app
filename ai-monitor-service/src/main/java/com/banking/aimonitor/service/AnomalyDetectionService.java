package com.banking.aimonitor.service;

import com.banking.aimonitor.domain.Anomaly;
import com.banking.common.event.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Rule-based anomaly detection over the transaction stream.
 *
 * <ul>
 *     <li><b>LARGE_AMOUNT</b> — a single transaction with amount &gt; 1,000,000.</li>
 *     <li><b>VELOCITY</b> — more than 5 transactions for the same account within 60s.</li>
 * </ul>
 *
 * Detected anomalies are held in an in-memory concurrent structure (no database).
 */
@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);

    static final BigDecimal LARGE_AMOUNT_THRESHOLD = BigDecimal.valueOf(1_000_000);
    static final Duration VELOCITY_WINDOW = Duration.ofSeconds(60);
    static final int VELOCITY_LIMIT = 5;

    private final List<Anomaly> anomalies = new CopyOnWriteArrayList<>();
    private final Map<String, Deque<Instant>> recentByAccount = new ConcurrentHashMap<>();

    /**
     * Inspect a single event and record any anomalies detected.
     *
     * @return the anomalies raised for this event (may be empty)
     */
    public List<Anomaly> inspect(TransactionEvent event) {
        List<Anomaly> raised = new java.util.ArrayList<>();
        String account = primaryAccount(event);
        BigDecimal amount = event.amount() == null ? BigDecimal.ZERO : event.amount();
        Instant now = event.timestamp() == null ? Instant.now() : event.timestamp();

        if (amount.compareTo(LARGE_AMOUNT_THRESHOLD) > 0) {
            raised.add(record(account, "LARGE_AMOUNT", amount, now,
                    "Transaction amount %s exceeds the large-amount threshold of %s."
                            .formatted(amount.toPlainString(), LARGE_AMOUNT_THRESHOLD.toPlainString())));
        }

        int count = registerAndCount(account, now);
        if (count > VELOCITY_LIMIT) {
            raised.add(record(account, "VELOCITY", amount, now,
                    "%d transactions observed for account %s within %d seconds (limit %d)."
                            .formatted(count, account, VELOCITY_WINDOW.toSeconds(), VELOCITY_LIMIT)));
        }

        if (!raised.isEmpty()) {
            log.info("Detected {} anomaly(ies) for account {} on event {}",
                    raised.size(), account, event.eventId());
        }
        return raised;
    }

    private Anomaly record(String account, String rule, BigDecimal amount, Instant when, String explanation) {
        Anomaly anomaly = new Anomaly(UUID.randomUUID().toString(), account, rule, amount, when, explanation);
        anomalies.add(anomaly);
        return anomaly;
    }

    private int registerAndCount(String account, Instant now) {
        Deque<Instant> window = recentByAccount.computeIfAbsent(account, k -> new ArrayDeque<>());
        synchronized (window) {
            window.addLast(now);
            Instant cutoff = now.minus(VELOCITY_WINDOW);
            while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
                window.pollFirst();
            }
            return window.size();
        }
    }

    private String primaryAccount(TransactionEvent event) {
        if (event.debitAccount() != null && !event.debitAccount().isBlank()) {
            return event.debitAccount();
        }
        if (event.creditAccount() != null && !event.creditAccount().isBlank()) {
            return event.creditAccount();
        }
        return "unknown";
    }

    public List<Anomaly> allAnomalies() {
        return anomalies.stream()
                .sorted(Comparator.comparing(Anomaly::detectedAt).reversed())
                .toList();
    }

    public List<Anomaly> anomaliesForAccount(String accountNumber) {
        return anomalies.stream()
                .filter(a -> a.accountNumber().equals(accountNumber))
                .sorted(Comparator.comparing(Anomaly::detectedAt).reversed())
                .toList();
    }

    /** Counts of anomalies grouped by rule. */
    public Map<String, Long> countsByRule() {
        return anomalies.stream()
                .collect(Collectors.groupingBy(Anomaly::rule, Collectors.counting()));
    }
}
