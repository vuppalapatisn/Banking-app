package com.banking.reporting.kafka;

import com.banking.common.event.KafkaTopics;
import com.banking.common.event.TransactionEvent;
import com.banking.reporting.service.AggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Subscribes to the committed-transaction topic and folds each event into the
 * daily aggregate store.
 */
@Component
public class TransactionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventListener.class);

    private final AggregationService aggregationService;

    public TransactionEventListener(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @KafkaListener(
            topics = KafkaTopics.TRANSACTIONS,
            containerFactory = "transactionEventKafkaListenerContainerFactory")
    public void onTransaction(TransactionEvent event) {
        if (event == null) {
            return;
        }
        log.debug("Aggregating transaction event {} ({})", event.transactionId(), event.type());
        aggregationService.record(event);
    }
}
