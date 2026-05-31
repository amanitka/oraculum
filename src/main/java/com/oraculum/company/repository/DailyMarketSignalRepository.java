package com.oraculum.company.repository;

import com.oraculum.company.domain.DailyMarketSignalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyMarketSignalRepository extends JpaRepository<DailyMarketSignalEntity, DailyMarketSignalEntity.DailyMarketSignalId> {
    List<DailyMarketSignalEntity> findByCompanyId(int companyId);
}