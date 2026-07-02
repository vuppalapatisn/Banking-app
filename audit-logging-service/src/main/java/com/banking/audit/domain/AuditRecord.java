package com.banking.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Immutable audit trail record. There is intentionally no setter and no update/delete
 * path exposed anywhere in the service.
 */
@Entity
@Table(name = "audit_records")
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String eventType;

    @Column(updatable = false)
    private String entityRef;

    @Column(length = 4000, updatable = false)
    private String payload;

    @Column(nullable = false, updatable = false)
    private String source;

    @Column(nullable = false, updatable = false)
    private Instant receivedAt;

    protected AuditRecord() {
        // for JPA
    }

    public AuditRecord(String eventType, String entityRef, String payload, String source, Instant receivedAt) {
        this.eventType = eventType;
        this.entityRef = entityRef;
        this.payload = payload;
        this.source = source;
        this.receivedAt = receivedAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEntityRef() {
        return entityRef;
    }

    public String getPayload() {
        return payload;
    }

    public String getSource() {
        return source;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
