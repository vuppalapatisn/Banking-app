package com.banking.core.repository;

import com.banking.core.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);

    List<LedgerEntry> findByTransactionId(String transactionId);
}
