package com.banking.core.web;

import com.banking.core.dto.CreditRequest;
import com.banking.core.dto.DebitRequest;
import com.banking.core.dto.TransactionResponse;
import com.banking.core.dto.TransferRequest;
import com.banking.core.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/credit")
    public TransactionResponse credit(@Valid @RequestBody CreditRequest request) {
        return TransactionResponse.from(transactionService.credit(request));
    }

    @PostMapping("/debit")
    public TransactionResponse debit(@Valid @RequestBody DebitRequest request) {
        return TransactionResponse.from(transactionService.debit(request));
    }

    @PostMapping("/transfer")
    public TransactionResponse transfer(@Valid @RequestBody TransferRequest request) {
        return TransactionResponse.from(transactionService.transfer(request));
    }

    @GetMapping("/account/{accountNumber}")
    public List<TransactionResponse> history(@PathVariable String accountNumber) {
        return transactionService.history(accountNumber).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
