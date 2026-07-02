package com.banking.aimonitor.web;

import com.banking.aimonitor.domain.Anomaly;
import com.banking.aimonitor.service.AnomalyDetectionService;
import com.banking.aimonitor.service.RiskAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final AnomalyDetectionService detectionService;
    private final RiskAnalysisService riskAnalysisService;

    public MonitorController(AnomalyDetectionService detectionService,
                             RiskAnalysisService riskAnalysisService) {
        this.detectionService = detectionService;
        this.riskAnalysisService = riskAnalysisService;
    }

    /** All detected anomalies, most recent first. */
    @GetMapping("/anomalies")
    public List<Anomaly> anomalies() {
        return detectionService.allAnomalies();
    }

    /** Request body for {@link #analyze(AnalyzeRequest)}. */
    public record AnalyzeRequest(String accountNumber) {
    }

    /** Response for {@link #analyze(AnalyzeRequest)}. */
    public record AnalyzeResponse(String accountNumber, int anomalyCount, String summary) {
    }

    /**
     * Natural-language risk summary for an account. Uses Claude when a real
     * {@code ANTHROPIC_API_KEY} is configured; otherwise returns a heuristic summary.
     */
    @PostMapping("/analyze")
    public AnalyzeResponse analyze(@RequestBody AnalyzeRequest request) {
        String account = request.accountNumber();
        List<Anomaly> forAccount = detectionService.anomaliesForAccount(account);
        String summary = riskAnalysisService.summarise(account, forAccount);
        return new AnalyzeResponse(account, forAccount.size(), summary);
    }

    /** Counts of anomalies grouped by detection rule. */
    @GetMapping("/health-summary")
    public Map<String, Long> healthSummary() {
        return detectionService.countsByRule();
    }
}
