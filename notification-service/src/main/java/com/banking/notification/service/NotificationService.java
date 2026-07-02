package com.banking.notification.service;

import com.banking.common.event.AccountEvent;
import com.banking.common.event.TransactionEvent;
import com.banking.notification.domain.Notification;
import com.banking.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * On a transaction event we notify both parties via SMS and EMAIL (simulated).
     */
    @Transactional
    public void handleTransaction(TransactionEvent event) {
        String account = event.debitAccount() != null ? event.debitAccount() : event.creditAccount();
        String subject = "Transaction " + event.status();
        String body = String.format(
                "A %s transaction of %s %s (ref %s) has been %s. Debit=%s Credit=%s.",
                event.type(), event.amount(), event.currency(), event.transactionId(),
                event.status(), event.debitAccount(), event.creditAccount());

        send("SMS", account, subject, body, account);
        send("EMAIL", account, subject, body, account);
    }

    /**
     * On account creation we send a welcome notification.
     */
    @Transactional
    public void handleAccount(AccountEvent event) {
        if (event.eventType() == AccountEvent.AccountEventType.CREATED) {
            String subject = "Welcome to the bank";
            String body = String.format(
                    "Welcome! Account %s for customer %s has been created.",
                    event.accountNumber(), event.customerId());
            send("EMAIL", event.accountNumber(), subject, body, event.accountNumber());
        } else {
            log.info("No notification configured for account event type {} on account {}",
                    event.eventType(), event.accountNumber());
        }
    }

    private void send(String channel, String recipient, String subject, String body, String relatedAccount) {
        // Simulate dispatch to an external gateway.
        log.info("[{}] -> {} | {} | {}", channel, recipient, subject, body);
        repository.save(new Notification(channel, recipient, subject, body, Instant.now(), relatedAccount));
    }

    @Transactional(readOnly = true)
    public List<Notification> findAll() {
        return repository.findAllByOrderBySentAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Notification> findByAccount(String account) {
        return repository.findByRelatedAccountOrderBySentAtDesc(account);
    }
}
