package com.banking.audit.service;

import com.banking.audit.domain.AuditRecord;
import com.banking.audit.repository.AuditRecordRepository;
import com.banking.common.event.AccountEvent;
import com.banking.common.event.TransactionEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRecordRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordTransaction(TransactionEvent event) {
        String payload = toJson(event);
        AuditRecord record = new AuditRecord(
                "TRANSACTION_" + event.type(),
                event.transactionId(),
                payload,
                "banking.transactions",
                Instant.now());
        repository.save(record);
        log.info("Audited transaction event {} for txn {}", event.eventId(), event.transactionId());
    }

    @Transactional
    public void recordAccount(AccountEvent event) {
        String payload = toJson(event);
        AuditRecord record = new AuditRecord(
                "ACCOUNT_" + event.eventType(),
                event.accountNumber(),
                payload,
                "banking.accounts",
                Instant.now());
        repository.save(record);
        log.info("Audited account event {} for account {}", event.eventId(), event.accountNumber());
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize event to JSON, storing toString fallback", e);
            return String.valueOf(event);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditRecord> findAll(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return repository.findAllByOrderByReceivedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditRecord> findByAccount(String account) {
        return repository.findByEntityRefOrderByReceivedAtDesc(account);
    }

    @Transactional(readOnly = true)
    public List<AuditRecord> findByType(String eventType) {
        return repository.findByEventTypeOrderByReceivedAtDesc(eventType);
    }
}
