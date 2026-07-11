package com.oraculum.company.repository;

import com.oraculum.company.domain.TickerSecDocumentStaleSyncEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TickerSecDocumentStaleSyncRepository extends JpaRepository<TickerSecDocumentStaleSyncEntity, TickerSecDocumentStaleSyncEntity.TickerSecDocumentStaleSyncId> {

    @Query("SELECT e FROM TickerSecDocumentStaleSyncEntity e ORDER BY e.lastRefreshAt ASC NULLS FIRST")
    List<TickerSecDocumentStaleSyncEntity> findStaleDocuments(Pageable pageable);
}
