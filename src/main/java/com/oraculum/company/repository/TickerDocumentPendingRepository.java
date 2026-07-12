package com.oraculum.company.repository;

import com.oraculum.company.domain.TickerDocumentPendingEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TickerDocumentPendingRepository extends JpaRepository<TickerDocumentPendingEntity, TickerDocumentPendingEntity.TickerDocumentPendingId> {

    @Query("SELECT e FROM TickerDocumentPendingEntity e")
    List<TickerDocumentPendingEntity> findPendingDocuments(Pageable pageable);

    List<TickerDocumentPendingEntity> findPendingByTickerAndMarket(@Param("ticker") String ticker, @Param("market") String market);
}
