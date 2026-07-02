package com.banking.product.service;

import com.banking.product.domain.Product;
import com.banking.product.dto.ProductRequest;
import com.banking.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for managing banking products and their configuration.
 */
@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Product findByCode(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new ProductNotFoundException(code));
    }

    @Transactional
    public Product create(ProductRequest request) {
        Product product = new Product(
                request.code(),
                request.name(),
                request.type(),
                request.interestRate(),
                request.monthlyFee(),
                request.dailyDebitLimit(),
                request.minBalance(),
                request.active());
        return repository.save(product);
    }

    @Transactional
    public Product update(String code, ProductRequest request) {
        Product product = findByCode(code);
        product.setName(request.name());
        product.setType(request.type());
        product.setInterestRate(request.interestRate());
        product.setMonthlyFee(request.monthlyFee());
        product.setDailyDebitLimit(request.dailyDebitLimit());
        product.setMinBalance(request.minBalance());
        product.setActive(request.active());
        return repository.save(product);
    }

    @Transactional
    public void delete(String code) {
        if (!repository.existsByCode(code)) {
            throw new ProductNotFoundException(code);
        }
        repository.deleteByCode(code);
    }
}
