package com.oraculum.company.repository;

import com.oraculum.company.domain.SecHoldingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecHoldingRepository extends JpaRepository<SecHoldingEntity, SecHoldingEntity.SecHoldingId> {

    @Query("""
            SELECT DISTINCT TRIM(h.cusip) FROM SecHoldingEntity h
            WHERE h.cusip IS NOT NULL
              AND LENGTH(TRIM(h.cusip)) = 9
              AND h.optionType IS NULL
              AND (h.sharesOrPrnType IS NULL OR h.sharesOrPrnType = 'SH')
            """)
    List<String> findDistinctCusips();
}
