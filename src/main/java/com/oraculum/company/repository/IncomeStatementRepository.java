package com.oraculum.company.repository;

import com.oraculum.company.domain.IncomeStatementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IncomeStatementRepository extends JpaRepository<IncomeStatementEntity, String> {
    List<IncomeStatementEntity> findByCompanyIdAndReportDateAfter(int companyId, LocalDate after);
}
