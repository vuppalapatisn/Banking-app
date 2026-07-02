package com.banking.core.repository;

import com.banking.core.domain.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {

    Optional<TransactionRecord> findByTransactionId(String transactionId);

    List<TransactionRecord> findTop50ByDebitAccountOrCreditAccountOrderByCreatedAtDesc(String debitAccount, String creditAccount);
}
