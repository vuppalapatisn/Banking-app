package com.banking.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SMS / EMAIL / PUSH */
    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String recipient;

    private String subject;

    @Column(length = 2000)
    private String body;

    @Column(nullable = false)
    private Instant sentAt;

    private String relatedAccount;

    protected Notification() {
        // for JPA
    }

    public Notification(String channel, String recipient, String subject, String body,
                        Instant sentAt, String relatedAccount) {
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.sentAt = sentAt;
        this.relatedAccount = relatedAccount;
    }

    public Long getId() {
        return id;
    }

    public String getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public String getRelatedAccount() {
        return relatedAccount;
    }
}
