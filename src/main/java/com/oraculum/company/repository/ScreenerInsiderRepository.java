package com.oraculum.company.repository;

import com.oraculum.company.domain.CompanyOverviewBaseEntity;
import com.oraculum.company.domain.ScreenerInsiderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreenerInsiderRepository extends JpaRepository<ScreenerInsiderEntity, CompanyOverviewBaseEntity.ScreenerId> {
}
