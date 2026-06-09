package com.oraculum.company.repository;

import com.oraculum.company.domain.BaseScreenerEntity;
import com.oraculum.company.domain.ScreenerQualityCompoundersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreenerQualityCompoundersRepository extends JpaRepository<ScreenerQualityCompoundersEntity, BaseScreenerEntity.ScreenerId> {
}
