package com.banking.product.config;

import com.banking.product.domain.Product;
import com.banking.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds a few sample banking products on startup when the store is empty.
 */
@Component
public class ProductDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductDataSeeder.class);

    private final ProductRepository repository;

    public ProductDataSeeder(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        List<Product> seed = List.of(
                new Product("SAV-STD", "Standard Savings", "SAVINGS",
                        new BigDecimal("3.50"), new BigDecimal("0.00"),
                        new BigDecimal("5000.00"), new BigDecimal("100.00"), true),
                new Product("CUR-BIZ", "Business Current Account", "CURRENT",
                        new BigDecimal("0.00"), new BigDecimal("15.00"),
                        new BigDecimal("50000.00"), new BigDecimal("1000.00"), true),
                new Product("FD-12M", "12-Month Fixed Deposit", "FIXED_DEPOSIT",
                        new BigDecimal("6.75"), new BigDecimal("0.00"),
                        new BigDecimal("0.00"), new BigDecimal("10000.00"), true),
                new Product("LN-PERS", "Personal Loan", "LOAN",
                        new BigDecimal("12.00"), new BigDecimal("0.00"),
                        new BigDecimal("0.00"), new BigDecimal("0.00"), false));

        repository.saveAll(seed);
        log.info("Seeded {} sample products", seed.size());
    }
}
