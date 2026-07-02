package com.banking.notification.web;

import com.banking.notification.domain.Notification;
import com.banking.notification.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<Notification> all() {
        return service.findAll();
    }

    @GetMapping("/account/{acct}")
    public List<Notification> byAccount(@PathVariable("acct") String acct) {
        return service.findByAccount(acct);
    }
}
