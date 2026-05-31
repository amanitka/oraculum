package com.oraculum.company.repository;

import com.oraculum.company.domain.IncomeStatementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomeStatementRepository extends JpaRepository<IncomeStatementEntity, String> {
    Page<IncomeStatementEntity> findByCompanyIdAndVariant(int companyId, String variant, Pageable pageable);
}