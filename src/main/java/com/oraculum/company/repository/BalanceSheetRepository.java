package com.oraculum.company.repository;

import com.oraculum.company.domain.BalanceSheetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceSheetRepository extends JpaRepository<BalanceSheetEntity, String> {
    Page<BalanceSheetEntity> findByCompanyIdAndVariant(int companyId, String variant, Pageable pageable);
}