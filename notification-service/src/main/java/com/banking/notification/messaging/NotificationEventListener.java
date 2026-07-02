package com.banking.notification.messaging;

import com.banking.common.event.AccountEvent;
import com.banking.common.event.TransactionEvent;
import com.banking.common.event.KafkaTopics;
import com.banking.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService service;

    public NotificationEventListener(NotificationService service) {
        this.service = service;
    }

    @KafkaListener(
            topics = KafkaTopics.TRANSACTIONS,
            containerFactory = "transactionEventListenerFactory")
    public void onTransaction(TransactionEvent event) {
        log.info("Received transaction event {}", event.eventId());
        service.handleTransaction(event);
    }

    @KafkaListener(
            topics = KafkaTopics.ACCOUNTS,
            containerFactory = "accountEventListenerFactory")
    public void onAccount(AccountEvent event) {
        log.info("Received account event {}", event.eventId());
        service.handleAccount(event);
    }
}
