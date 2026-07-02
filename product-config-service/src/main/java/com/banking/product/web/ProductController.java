package com.banking.product.web;

import com.banking.product.dto.ProductRequest;
import com.banking.product.dto.ProductResponse;
import com.banking.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for banking product setup and configuration.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductResponse> getAll() {
        return service.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/{code}")
    public ProductResponse getByCode(@PathVariable String code) {
        return ProductResponse.from(service.findByCode(code));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(service.create(request));
    }

    @PutMapping("/{code}")
    public ProductResponse update(@PathVariable String code, @Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(service.update(code, request));
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String code) {
        service.delete(code);
    }
}
