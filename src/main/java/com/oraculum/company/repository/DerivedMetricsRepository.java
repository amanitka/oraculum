package com.oraculum.company.repository;

import com.oraculum.company.domain.DerivedMetricsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DerivedMetricsRepository extends JpaRepository<DerivedMetricsEntity, String> {
    List<DerivedMetricsEntity> findByCompanyIdAndReportDateAfter(int companyId, LocalDate after);
}