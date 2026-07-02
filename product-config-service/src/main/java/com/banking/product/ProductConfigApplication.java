package com.banking.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Product and Configuration Service. Owns product setup,
 * interest rates, charges and fees, and limits and rules.
 */
@SpringBootApplication
public class ProductConfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductConfigApplication.class, args);
    }
}
