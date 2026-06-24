package com.oraculum.company.repository;

import com.oraculum.company.domain.InsiderTransactionSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsiderTransactionSummaryRepository extends JpaRepository<InsiderTransactionSummaryEntity, String> {
}
