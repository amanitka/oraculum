package com.oraculum.company.repository;

import com.oraculum.company.domain.TickerDocumentSyncStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TickerDocumentSyncStatusRepository extends JpaRepository<TickerDocumentSyncStatusEntity, TickerDocumentSyncStatusEntity.TickerDocumentSyncStatusId> {
    List<TickerDocumentSyncStatusEntity> findByTickerInAndMarket(List<String> tickers, String market);
}
