package com.banking.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * A banking product definition together with its configuration: pricing
 * (interest rate, monthly fee), limits (daily debit limit, minimum balance)
 * and lifecycle state.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String type;

    private BigDecimal interestRate;

    private BigDecimal monthlyFee;

    private BigDecimal dailyDebitLimit;

    private BigDecimal minBalance;

    private boolean active;

    protected Product() {
    }

    public Product(String code, String name, String type, BigDecimal interestRate, BigDecimal monthlyFee,
                   BigDecimal dailyDebitLimit, BigDecimal minBalance, boolean active) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.interestRate = interestRate;
        this.monthlyFee = monthlyFee;
        this.dailyDebitLimit = dailyDebitLimit;
        this.minBalance = minBalance;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public BigDecimal getMonthlyFee() {
        return monthlyFee;
    }

    public void setMonthlyFee(BigDecimal monthlyFee) {
        this.monthlyFee = monthlyFee;
    }

    public BigDecimal getDailyDebitLimit() {
        return dailyDebitLimit;
    }

    public void setDailyDebitLimit(BigDecimal dailyDebitLimit) {
        this.dailyDebitLimit = dailyDebitLimit;
    }

    public BigDecimal getMinBalance() {
        return minBalance;
    }

    public void setMinBalance(BigDecimal minBalance) {
        this.minBalance = minBalance;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
