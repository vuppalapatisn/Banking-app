package com.banking.audit.web;

import com.banking.audit.domain.AuditRecord;
import com.banking.audit.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditRecord> all(@RequestParam(name = "limit", defaultValue = "100") int limit) {
        return service.findAll(limit);
    }

    @GetMapping("/account/{acct}")
    public List<AuditRecord> byAccount(@PathVariable("acct") String acct) {
        return service.findByAccount(acct);
    }

    @GetMapping("/type/{eventType}")
    public List<AuditRecord> byType(@PathVariable("eventType") String eventType) {
        return service.findByType(eventType);
    }
}
