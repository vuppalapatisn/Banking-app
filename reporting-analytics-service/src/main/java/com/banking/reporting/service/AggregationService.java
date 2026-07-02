package com.banking.reporting.service;

import com.banking.common.event.TransactionEvent;
import com.banking.common.event.TransactionType;
import com.banking.reporting.domain.DailyAggregate;
import com.banking.reporting.repository.DailyAggregateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Applies incoming transaction events to the daily aggregate store and serves the
 * read models backing the reporting endpoints.
 */
@Service
public class AggregationService {

    private final DailyAggregateRepository repository;

    public AggregationService(DailyAggregateRepository repository) {
        this.repository = repository;
    }

    /**
     * Upserts the daily aggregate buckets touched by a transaction event. A transfer
     * touches both the debit and credit accounts; a plain credit/debit touches the one
     * account that is populated.
     */
    @Transactional
    public void record(TransactionEvent event) {
        if (event == null || event.timestamp() == null) {
            return;
        }
        LocalDate date = event.timestamp().atZone(ZoneOffset.UTC).toLocalDate();
        TransactionType type = event.type();

        if (event.debitAccount() != null && !event.debitAccount().isBlank()) {
            upsert(date, event.debitAccount(), type, event.amount());
        }
        if (event.creditAccount() != null && !event.creditAccount().isBlank()) {
            upsert(date, event.creditAccount(), type, event.amount());
        }
    }

    private void upsert(LocalDate date, String account, TransactionType type, java.math.BigDecimal amount) {
        DailyAggregate aggregate = repository
                .findByReportDateAndAccountAndType(date, account, type)
                .orElseGet(() -> new DailyAggregate(date, account, type));
        aggregate.apply(amount);
        repository.save(aggregate);
    }

    @Transactional(readOnly = true)
    public List<DailyAggregate> byDate(LocalDate date) {
        return repository.findByReportDate(date);
    }

    @Transactional(readOnly = true)
    public List<DailyAggregate> byAccount(String account) {
        return repository.findByAccount(account);
    }

    @Transactional(readOnly = true)
    public List<DailyAggregate> all() {
        return repository.findAll();
    }
}
