package com.oraculum.company.repository;

import com.oraculum.company.domain.CompanyOverviewBaseEntity;
import com.oraculum.company.domain.CompanyOverviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyOverviewRepository extends JpaRepository<CompanyOverviewEntity, CompanyOverviewBaseEntity.ScreenerId> {
}
