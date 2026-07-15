package com.oraculum.company.repository;

import com.oraculum.company.domain.TickerDocumentPendingEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TickerDocumentPendingRepository extends JpaRepository<TickerDocumentPendingEntity, TickerDocumentPendingEntity.TickerDocumentPendingId> {

    @Query("SELECT e FROM TickerDocumentPendingEntity e WHERE e.status IN (com.oraculum.company.api.domain.TickerDocumentProcessingStatus.PENDING, com.oraculum.company.api.domain.TickerDocumentProcessingStatus.FAILED) AND e.documentPriority <= :maxPriority")
    List<TickerDocumentPendingEntity> findPendingDocuments(@Param("maxPriority") int maxPriority, Pageable pageable);

    @Query("SELECT e FROM TickerDocumentPendingEntity e WHERE e.ticker = :ticker AND e.market = :market AND e.status IN (com.oraculum.company.api.domain.TickerDocumentProcessingStatus.PENDING, com.oraculum.company.api.domain.TickerDocumentProcessingStatus.FAILED) AND e.documentPriority <= :maxPriority")
    List<TickerDocumentPendingEntity> findByTickerAndMarketAndDocumentPriorityLessThanEqual(@Param("ticker") String ticker, @Param("market") String market, @Param("maxPriority") Integer maxPriority);
}
