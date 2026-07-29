package com.oraculum.company.repository;

import com.oraculum.company.domain.TickerSecDocumentSyncEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TickerSecDocumentSyncRepository extends JpaRepository<TickerSecDocumentSyncEntity, TickerSecDocumentSyncEntity.TickerSecDocumentSyncId> {

    @Query("SELECT e FROM TickerSecDocumentSyncEntity e WHERE e.isStale = true ORDER BY e.lastRefreshAt ASC NULLS FIRST")
    List<TickerSecDocumentSyncEntity> findStaleDocuments(Pageable pageable);
    
    @Query("SELECT e FROM TickerSecDocumentSyncEntity e WHERE e.ticker IN :tickers")
    List<TickerSecDocumentSyncEntity> findByTickers(@Param("tickers") List<String> tickers);
}
