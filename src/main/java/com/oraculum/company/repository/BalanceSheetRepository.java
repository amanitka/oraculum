package com.oraculum.company.repository;

import com.oraculum.company.domain.BalanceSheetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceSheetRepository extends JpaRepository<BalanceSheetEntity, Integer> {
    Page<BalanceSheetEntity> findByTickerAndVariant(String ticker, String variant, Pageable pageable);
}