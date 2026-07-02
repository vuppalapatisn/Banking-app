package com.banking.core.domain;

import com.banking.common.event.TransactionStatus;
import com.banking.common.event.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The header record for a posted transaction. The matching money movement is
 * captured as {@link LedgerEntry} rows sharing the same transactionId.
 */
@Entity
@Table(name = "transactions")
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionStatus status;

    @Column(length = 20)
    private String debitAccount;

    @Column(length = 20)
    private String creditAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 140)
    private String narration;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected TransactionRecord() {
    }

    public TransactionRecord(String transactionId, TransactionType type, TransactionStatus status,
                             String debitAccount, String creditAccount, BigDecimal amount,
                             String currency, String narration) {
        this.transactionId = transactionId;
        this.type = type;
        this.status = status;
        this.debitAccount = debitAccount;
        this.creditAccount = creditAccount;
        this.amount = amount;
        this.currency = currency;
        this.narration = narration;
    }

    public Long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getDebitAccount() {
        return debitAccount;
    }

    public String getCreditAccount() {
        return creditAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getNarration() {
        return narration;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
