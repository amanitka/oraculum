package com.oraculum.company.service.impl;

import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.event.TickerDocumentLoadEvent;
import com.oraculum.company.domain.TickerDocumentSyncStatusEntity;
import com.oraculum.company.repository.TickerDocumentSyncStatusRepository;
import com.oraculum.load.api.dto.DataFileStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class TickerDocumentSyncStatusService {

    private final TickerDocumentSyncStatusRepository repository;

    private TickerDocumentType getDocumentType(String fileType) {
        try {
            return TickerDocumentType.valueOf(fileType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown document type: {}", fileType);
            return null;
        }
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

    private void processDataFileStatus(DataFileStatus status, LocalDate processingDate) {
        var docType = getDocumentType(status.fileType());
        if (docType == null) {
            return;
        }
        var id = getTickerDocumentSyncStatusId(docType, status);
        var entity = getTickerDocumentSyncStatusEntity(id);
        entity.setStatus(status.status());
        entity.setExtractionStatus(status.extractionStatus());
        entity.setMessage(status.message());
        if (status.latestProcessedDate() != null) {
            entity.setLastProcessedFileDate(status.latestProcessedDate());
            entity.setLastFileRefreshDate(processingDate);
        }
        entity.setLastRefreshDate(processingDate);

        repository.save(entity);
    }

    public void processDocumentLoadEvent(TickerDocumentLoadEvent event) {
        LocalDate processingDate = LocalDate.now(ZoneOffset.UTC);
        for (DataFileStatus status : event.fileStatuses()) {
            processDataFileStatus(status, processingDate);
        }
    }
}
