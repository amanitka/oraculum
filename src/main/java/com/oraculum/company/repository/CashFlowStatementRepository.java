package com.oraculum.company.repository;

import com.oraculum.company.domain.CashFlowStatementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CashFlowStatementRepository extends JpaRepository<CashFlowStatementEntity, Integer> {
    List<CashFlowStatementEntity> findByTicker(String ticker);
}