package com.oraculum.company.repository;

import com.oraculum.company.domain.CompanyOverviewBaseEntity;
import com.oraculum.company.domain.ScreenerFinancialTrendEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreenerFinancialTrendRepository extends JpaRepository<ScreenerFinancialTrendEntity, CompanyOverviewBaseEntity.ScreenerId> {
}
