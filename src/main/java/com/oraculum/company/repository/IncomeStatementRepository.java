package com.oraculum.company.repository;

import com.oraculum.company.domain.IncomeStatementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeStatementRepository extends JpaRepository<IncomeStatementEntity, Integer> {
    List<IncomeStatementEntity> findByTicker(String ticker);
}