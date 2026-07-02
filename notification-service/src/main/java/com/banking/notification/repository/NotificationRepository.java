package com.banking.notification.repository;

import com.banking.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRelatedAccountOrderBySentAtDesc(String relatedAccount);

    List<Notification> findAllByOrderBySentAtDesc();
}
