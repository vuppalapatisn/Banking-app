package com.banking.reporting.repository;

import com.banking.common.event.TransactionType;
import com.banking.reporting.domain.DailyAggregate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyAggregateRepository extends JpaRepository<DailyAggregate, Long> {

    Optional<DailyAggregate> findByReportDateAndAccountAndType(
            LocalDate reportDate, String account, TransactionType type);

    List<DailyAggregate> findByReportDate(LocalDate reportDate);

    List<DailyAggregate> findByAccount(String account);
}
