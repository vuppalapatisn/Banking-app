package com.banking.aimonitor.messaging;

import com.banking.aimonitor.service.AnomalyDetectionService;
import com.banking.common.event.KafkaTopics;
import com.banking.common.event.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes committed {@link TransactionEvent}s and hands each to the anomaly detector.
 */
@Component
public class TransactionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventListener.class);

    private final AnomalyDetectionService detectionService;

    public TransactionEventListener(AnomalyDetectionService detectionService) {
        this.detectionService = detectionService;
    }

    @KafkaListener(
            topics = KafkaTopics.TRANSACTIONS,
            containerFactory = "transactionEventListenerFactory")
    public void onTransaction(TransactionEvent event) {
        log.debug("Inspecting transaction event {}", event.eventId());
        detectionService.inspect(event);
    }
}
