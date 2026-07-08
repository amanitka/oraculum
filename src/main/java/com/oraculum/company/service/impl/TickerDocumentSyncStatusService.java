package com.oraculum.company.service.impl;

import com.oraculum.company.api.domain.SyncExtractionStatus;
import com.oraculum.company.api.domain.SyncStatus;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.event.TickerDocumentLoadEvent;
import com.oraculum.company.domain.TickerDocumentSyncStatusEntity;
import com.oraculum.company.repository.TickerDocumentSyncStatusRepository;
import com.oraculum.load.api.dto.DataFileStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class TickerDocumentSyncStatusService {

    private final TickerDocumentSyncStatusRepository repository;

    private TickerDocumentType getDocumentType(String fileType) {
        return TickerDocumentType.fromString(fileType).orElseGet(() -> {
            log.warn("Unknown document type: {}", fileType);
            return null;
        });
    }

    private TickerDocumentSyncStatusEntity.TickerDocumentSyncStatusId getTickerDocumentSyncStatusId(TickerDocumentType documentType, DataFileStatus status) {
        return new TickerDocumentSyncStatusEntity.TickerDocumentSyncStatusId(status.ticker(), status.market(), status.source(), documentType);
    }

    private TickerDocumentSyncStatusEntity getTickerDocumentSyncStatusEntity(TickerDocumentSyncStatusEntity.TickerDocumentSyncStatusId id) {
        return repository.findById(id).orElseGet(() ->
                TickerDocumentSyncStatusEntity.builder()
                        .ticker(id.getTicker())
                        .market(id.getMarket())
                        .source(id.getSource())
                        .documentType(id.getDocumentType())
                        .build()
        );
    }

    private void processDataFileStatus(DataFileStatus status, OffsetDateTime processingDate) {
        var docType = getDocumentType(status.fileType());
        if (docType == null) {
            return;
        }
        var id = getTickerDocumentSyncStatusId(docType, status);
        var entity = getTickerDocumentSyncStatusEntity(id);
        SyncStatus.fromString(status.status())
                .ifPresentOrElse(entity::setStatus, () -> log.warn("Unknown sync status: {}", status.status()));
        SyncExtractionStatus.fromString(status.extractionStatus())
                .ifPresentOrElse(entity::setExtractionStatus, () -> log.warn("Unknown extraction status: {}", status.extractionStatus()));
        entity.setMessage(status.message());
        if (status.latestProcessedDate() != null) {
            entity.setLastProcessedFileDate(status.latestProcessedDate());
            entity.setLastFileRefreshAt(processingDate);
        }
        entity.setLastRefreshAt(processingDate);

        repository.save(entity);
    }

    public void processDocumentLoadEvent(TickerDocumentLoadEvent event) {
        OffsetDateTime processingDate = OffsetDateTime.now(ZoneOffset.UTC);
        for (DataFileStatus status : event.fileStatuses()) {
            processDataFileStatus(status, processingDate);
        }
    }
}
