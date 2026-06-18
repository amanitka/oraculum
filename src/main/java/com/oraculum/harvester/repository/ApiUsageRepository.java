package com.oraculum.harvester.repository;

import com.oraculum.harvester.domain.ApiUsageEntity;
import com.oraculum.harvester.domain.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiUsageRepository extends JpaRepository<ApiUsageEntity, ProviderType> {
}
