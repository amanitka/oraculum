package com.oraculum.company.repository;

import com.oraculum.company.domain.CashFlowStatementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashFlowStatementRepository extends JpaRepository<CashFlowStatementEntity, String> {
    Page<CashFlowStatementEntity> findByCompanyIdAndVariant(int companyId, String variant, Pageable pageable);
}