package com.oraculum.company.repository;

import com.oraculum.company.domain.BalanceSheetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BalanceSheetRepository extends JpaRepository<BalanceSheetEntity, String> {
    List<BalanceSheetEntity> findByCompanyIdAndReportDateAfter(int companyId, LocalDate after);
}