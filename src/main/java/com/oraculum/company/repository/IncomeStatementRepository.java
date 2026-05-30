package com.oraculum.company.repository;

import com.oraculum.company.domain.IncomeStatementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomeStatementRepository extends JpaRepository<IncomeStatementEntity, Integer> {
    Page<IncomeStatementEntity> findByTickerAndVariant(String ticker, String variant, Pageable pageable);
}