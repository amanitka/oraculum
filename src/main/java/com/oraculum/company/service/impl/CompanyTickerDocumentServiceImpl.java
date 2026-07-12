package com.oraculum.company.service.impl;

import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.dto.TickerDocumentDto;
import com.oraculum.company.api.dto.TickerDocumentRawDto;
import com.oraculum.company.api.dto.TickerDocumentSyncStatusDto;
import com.oraculum.company.domain.TickerDocumentEntity;
import com.oraculum.company.domain.TickerDocumentPendingEntity;
import com.oraculum.company.domain.TickerDocumentSyncStatusEntity;
import com.oraculum.company.domain.TickerSecDocumentStaleSyncEntity;
import com.oraculum.company.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyTickerDocumentServiceImpl implements CompanyTickerDocumentApi {

    private final TickerDocumentSyncStatusRepository repository;
    private final TickerSecDocumentStaleSyncRepository staleSyncRepository;
    private final TickerDocumentPendingRepository pendingRepository;
    private final TickerDocumentRawRepository rawRepository;
    private final TickerDocumentRepository summaryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TickerDocumentSyncStatusDto> getSyncStatusesByTickersAndMarket(List<String> tickers, String market) {
        return repository.findByTickerInAndMarket(tickers, market).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TickerDocumentSyncStatusDto> getStaleSecDocuments(int limit) {
        return staleSyncRepository.findStaleDocuments(PageRequest.of(0, limit)).stream()
                .map(this::mapStaleToDto)
                .toList();
    }

    private TickerDocumentSyncStatusDto mapToDto(TickerDocumentSyncStatusEntity entity) {
        return TickerDocumentSyncStatusDto.builder()
                .ticker(entity.getTicker())
                .market(entity.getMarket())
                .documentType(entity.getDocumentType())
                .lastProcessedFileDate(entity.getLastProcessedFileDate())
                .build();
    }

    private TickerDocumentSyncStatusDto mapStaleToDto(TickerSecDocumentStaleSyncEntity entity) {
        return TickerDocumentSyncStatusDto.builder()
                .ticker(entity.getTicker())
                .market(entity.getMarket())
                .cik(entity.getCik())
                .documentType(entity.getDocumentType())
                .lastProcessedFileDate(entity.getLastProcessedFileDate())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TickerDocumentRawDto> getPendingRawDocuments(int limit) {
        return pendingRepository.findPendingDocuments(PageRequest.of(0, limit)).stream()
                .map(this::mapPendingToDto)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDocumentSummary(TickerDocumentDto summary) {
        TickerDocumentEntity entity = TickerDocumentEntity.builder()
                .id(summary.getId())
                .ticker(summary.getTicker())
                .market(summary.getMarket())
                .documentType(summary.getDocumentType())
                .documentSubtype(summary.getDocumentSubtype())
                .reportPeriod(summary.getReportPeriod())
                .summary(summary.getSummary())
                .sentimentScore(summary.getSentimentScore())
                .build();
        summaryRepository.save(entity);

        rawRepository.updateStatus(summary.getId(), summary.getReportPeriod(), "PROCESSED");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRawDocumentStatus(String id, LocalDate reportPeriod, String status) {
        rawRepository.updateStatus(id, reportPeriod, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TickerDocumentRawDto> getPendingRawDocumentsByTicker(String ticker, String market) {
        return pendingRepository.findPendingByTickerAndMarket(ticker, market).stream()
                .map(this::mapPendingToDto)
                .toList();
    }

    private TickerDocumentRawDto mapPendingToDto(TickerDocumentPendingEntity entity) {
        return TickerDocumentRawDto.builder()
                .id(entity.getId())
                .ticker(entity.getTicker())
                .market(entity.getMarket())
                .documentType(entity.getDocumentType())
                .documentSubtype(entity.getDocumentSubtype())
                .reportPeriod(entity.getReportPeriod())
                .filingDate(entity.getFilingDate())
                .content(entity.getContent())
                .build();
    }
}

