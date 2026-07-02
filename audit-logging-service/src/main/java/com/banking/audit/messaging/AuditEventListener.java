package com.banking.audit.messaging;

import com.banking.audit.service.AuditService;
import com.banking.common.event.AccountEvent;
import com.banking.common.event.KafkaTopics;
import com.banking.common.event.TransactionEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener {

    private final AuditService service;

    public AuditEventListener(AuditService service) {
        this.service = service;
    }

    @KafkaListener(
            topics = KafkaTopics.TRANSACTIONS,
            containerFactory = "transactionEventListenerFactory")
    public void onTransaction(TransactionEvent event) {
        service.recordTransaction(event);
    }

    @KafkaListener(
            topics = KafkaTopics.ACCOUNTS,
            containerFactory = "accountEventListenerFactory")
    public void onAccount(AccountEvent event) {
        service.recordAccount(event);
    }
}
