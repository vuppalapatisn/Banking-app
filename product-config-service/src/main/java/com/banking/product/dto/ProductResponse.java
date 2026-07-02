package com.banking.product.dto;

import com.banking.product.domain.Product;

import java.math.BigDecimal;

/**
 * Representation of a product returned by the REST API.
 */
public record ProductResponse(
        Long id,
        String code,
        String name,
        String type,
        BigDecimal interestRate,
        BigDecimal monthlyFee,
        BigDecimal dailyDebitLimit,
        BigDecimal minBalance,
        boolean active) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getType(),
                product.getInterestRate(),
                product.getMonthlyFee(),
                product.getDailyDebitLimit(),
                product.getMinBalance(),
                product.isActive());
    }
}
