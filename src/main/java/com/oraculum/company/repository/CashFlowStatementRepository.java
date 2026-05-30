package com.oraculum.company.repository;

import com.oraculum.company.domain.CashFlowStatementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashFlowStatementRepository extends JpaRepository<CashFlowStatementEntity, Integer> {
    Page<CashFlowStatementEntity> findByTickerAndVariant(String ticker, String variant, Pageable pageable);
}