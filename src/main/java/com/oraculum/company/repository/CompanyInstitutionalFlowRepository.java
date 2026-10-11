package com.oraculum.company.repository;

import com.oraculum.company.domain.CompanyInstitutionalFlowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CompanyInstitutionalFlowRepository extends JpaRepository<CompanyInstitutionalFlowEntity, CompanyInstitutionalFlowEntity.CompanyHoldingPeriodId> {

    List<CompanyInstitutionalFlowEntity> findByQuarterIndex(long quarterIndex);

    List<CompanyInstitutionalFlowEntity> findByReportPeriod(LocalDate reportPeriod);

    @Query("SELECT DISTINCT f.reportPeriod FROM CompanyInstitutionalFlowEntity f ORDER BY f.reportPeriod DESC")
    List<LocalDate> findDistinctReportPeriods();

    List<CompanyInstitutionalFlowEntity> findByCompanyIdOrderByReportPeriodDesc(int companyId);
}
