package com.oraculum.company.repository;

import com.oraculum.company.domain.BaseScreenerEntity;
import com.oraculum.company.domain.ScreenerUndervaluedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreenerUndervaluedRepository extends JpaRepository<ScreenerUndervaluedEntity, BaseScreenerEntity.ScreenerId> {
}
