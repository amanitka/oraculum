package com.oraculum.company.repository;

import com.oraculum.company.domain.CompanyFinancialRatiosEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CompanyFinancialRatiosRepository extends JpaRepository<CompanyFinancialRatiosEntity, String> {
    List<CompanyFinancialRatiosEntity> findByCompanyIdAndReportDateAfter(int companyId, LocalDate after);
}