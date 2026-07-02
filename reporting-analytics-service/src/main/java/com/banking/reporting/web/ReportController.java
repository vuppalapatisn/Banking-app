package com.banking.reporting.web;

import com.banking.common.event.TransactionType;
import com.banking.reporting.domain.DailyAggregate;
import com.banking.reporting.service.AggregationService;
import com.banking.reporting.web.dto.AggregateResponse;
import com.banking.reporting.web.dto.SummaryResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only reporting API backed by the daily aggregate store.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final AggregationService aggregationService;

    public ReportController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    /** Daily report: every aggregate bucket for the given date. */
    @GetMapping("/daily")
    public List<AggregateResponse> daily(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return aggregationService.byDate(date).stream()
                .map(AggregateResponse::from)
                .toList();
    }

    /** Summary: totals by transaction type across all recorded activity. */
    @GetMapping("/summary")
    public List<SummaryResponse> summary() {
        Map<TransactionType, long[]> counts = new EnumMap<>(TransactionType.class);
        Map<TransactionType, BigDecimal> totals = new EnumMap<>(TransactionType.class);

        for (DailyAggregate aggregate : aggregationService.all()) {
            counts.computeIfAbsent(aggregate.getType(), t -> new long[1])[0] += aggregate.getTransactionCount();
            totals.merge(aggregate.getType(), aggregate.getTotalAmount(), BigDecimal::add);
        }

        return counts.entrySet().stream()
                .map(e -> new SummaryResponse(
                        e.getKey(),
                        e.getValue()[0],
                        totals.getOrDefault(e.getKey(), BigDecimal.ZERO)))
                .toList();
    }

    /** Per-account report: every aggregate bucket for the given account. */
    @GetMapping("/account/{acct}")
    public List<AggregateResponse> account(@PathVariable("acct") String acct) {
        return aggregationService.byAccount(acct).stream()
                .map(AggregateResponse::from)
                .toList();
    }
}
