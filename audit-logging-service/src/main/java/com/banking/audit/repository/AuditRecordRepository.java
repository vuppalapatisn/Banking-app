package com.banking.audit.repository;

import com.banking.audit.domain.AuditRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findAllByOrderByReceivedAtDesc(Pageable pageable);

    List<AuditRecord> findByEntityRefOrderByReceivedAtDesc(String entityRef);

    List<AuditRecord> findByEventTypeOrderByReceivedAtDesc(String eventType);
}
