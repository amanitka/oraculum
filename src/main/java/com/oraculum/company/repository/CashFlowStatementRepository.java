package com.oraculum.company.repository;

import com.oraculum.company.domain.CashFlowStatementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CashFlowStatementRepository extends JpaRepository<CashFlowStatementEntity, String> {
    List<CashFlowStatementEntity> findByCompanyIdAndReportDateAfter(int companyId, LocalDate after);
}