package com.oraculum.company.repository;

import com.oraculum.company.domain.TickerDocumentSyncStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TickerDocumentSyncStatusRepository extends JpaRepository<TickerDocumentSyncStatusEntity, TickerDocumentSyncStatusEntity.TickerDocumentSyncStatusId> {
}
