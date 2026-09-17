package com.oraculum.company.repository;

import com.oraculum.company.domain.SecHoldingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecHoldingRepository extends JpaRepository<SecHoldingEntity, SecHoldingEntity.SecHoldingId> {

    @Query("SELECT DISTINCT h.cusip FROM SecHoldingEntity h WHERE h.cusip IS NOT NULL AND TRIM(h.cusip) <> ''")
    List<String> findDistinctCusips();
}
