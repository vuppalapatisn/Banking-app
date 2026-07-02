package com.banking.core.service;

import com.banking.common.event.AccountEvent;
import com.banking.common.event.KafkaTopics;
import com.banking.common.event.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events to the event bus (Kafka) for the asynchronous integration
 * layer — notification, reporting, audit and the AI monitor. Publishing is
 * fire-and-forget: a broker outage never blocks or rolls back a committed transaction.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TransactionEvent event) {
        send(KafkaTopics.TRANSACTIONS, event.transactionId(), event);
    }

    public void publish(AccountEvent event) {
        send(KafkaTopics.ACCOUNTS, event.accountNumber(), event);
    }

    private void send(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.warn("Failed to publish event to {} (key={}): {}", topic, key, ex.getMessage());
                } else if (log.isDebugEnabled()) {
                    log.debug("Published event to {} (key={})", topic, key);
                }
            });
        } catch (Exception ex) {
            log.warn("Unable to publish event to {}: {}", topic, ex.getMessage());
        }
    }
}
