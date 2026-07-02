package com.banking.core.web;

import com.banking.core.domain.Account;
import com.banking.core.dto.AccountResponse;
import com.banking.core.dto.BalanceResponse;
import com.banking.core.dto.OpenAccountRequest;
import com.banking.core.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@Valid @RequestBody OpenAccountRequest request) {
        Account account = accountService.openAccount(request);
        return AccountResponse.from(account);
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse get(@PathVariable String accountNumber) {
        return AccountResponse.from(accountService.getAccount(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    public BalanceResponse balance(@PathVariable String accountNumber) {
        return accountService.getBalance(accountNumber);
    }
}
