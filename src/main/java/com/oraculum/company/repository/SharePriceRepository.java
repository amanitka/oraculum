package com.oraculum.company.repository;

import com.oraculum.company.domain.SharePriceEntity;
import com.oraculum.company.domain.SharePriceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SharePriceRepository extends JpaRepository<SharePriceEntity, SharePriceId> {
    List<SharePriceEntity> findByCompanyIdAndTradeDateAfter(int companyId, LocalDate after);
    
    @Query("SELECT MAX(sp.tradeDate) FROM SharePriceEntity sp")
    Optional<LocalDate> findMaxTradeDate();
}
