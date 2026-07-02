package com.banking.reporting.domain;

import com.banking.common.event.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Rolling daily aggregate keyed by (date, account, transaction type). Each committed
 * {@link com.banking.common.event.TransactionEvent} increments the transaction count
 * and adds to the running total amount for its bucket.
 */
@Entity
@Table(
        name = "daily_aggregate",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_aggregate",
                columnNames = {"report_date", "account", "type"}
        )
)
public class DailyAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "account", nullable = false)
    private String account;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(name = "transaction_count", nullable = false)
    private long transactionCount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    protected DailyAggregate() {
    }

    public DailyAggregate(LocalDate reportDate, String account, TransactionType type) {
        this.reportDate = reportDate;
        this.account = account;
        this.type = type;
        this.transactionCount = 0L;
        this.totalAmount = BigDecimal.ZERO;
    }

    public void apply(BigDecimal amount) {
        this.transactionCount += 1;
        this.totalAmount = this.totalAmount.add(amount == null ? BigDecimal.ZERO : amount);
    }

    public Long getId() {
        return id;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public String getAccount() {
        return account;
    }

    public TransactionType getType() {
        return type;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
