package com.oraculum.company.service.impl;

import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.domain.TickerDocumentProcessingStatus;
import com.oraculum.company.api.dto.TickerDocumentDto;
import com.oraculum.company.api.dto.TickerDocumentPendingDto;
import com.oraculum.company.api.dto.TickerDocumentSyncStatusDto;
import com.oraculum.company.api.dto.TickerKeyDto;
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
    private final TickerDocumentViewRepository latestSummaryRepository;

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
    public List<TickerDocumentPendingDto> getPendingRawDocuments(int limit, int maxPriority) {
        return pendingRepository.findPendingDocuments(maxPriority, PageRequest.of(0, limit)).stream()
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
                .filingDate(summary.getFilingDate())
                .sourceUrl(summary.getSourceUrl())
                .accessionNumber(summary.getAccessionNumber())
                .summary(summary.getSummary())
                .sentimentScore(summary.getSentimentScore())
                .build();
        summaryRepository.save(entity);

        rawRepository.updateStatus(summary.getId(), summary.getReportPeriod(), TickerDocumentProcessingStatus.PROCESSED);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRawDocumentStatus(String id, LocalDate reportPeriod, TickerDocumentProcessingStatus status) {
        rawRepository.updateStatus(id, reportPeriod, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TickerDocumentPendingDto> getPendingRawDocumentsByTicker(TickerKeyDto tickerKey, int maxPriority) {
        return pendingRepository.findByTickerAndMarketAndDocumentPriorityLessThanEqual(tickerKey.ticker(), tickerKey.market(), maxPriority).stream()
                .map(this::mapPendingToDto)
                .toList();
    }

    private TickerDocumentPendingDto mapPendingToDto(TickerDocumentPendingEntity entity) {
        return TickerDocumentPendingDto.builder()
                .id(entity.getId())
                .ticker(entity.getTicker())
                .market(entity.getMarket())
                .documentType(entity.getDocumentType())
                .documentSubtype(entity.getDocumentSubtype())
                .reportPeriod(entity.getReportPeriod())
                .filingDate(entity.getFilingDate())
                .sourceUrl(entity.getSourceUrl())
                .accessionNumber(entity.getAccessionNumber())
                .content(entity.getContent())
                .documentPriority(entity.getDocumentPriority())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TickerDocumentDto> getDocumentsByTicker(TickerKeyDto tickerKey) {
        return latestSummaryRepository.findByTickerAndMarket(tickerKey.ticker(), tickerKey.market()).stream()
                .map(this::mapLatestSummaryToDto)
                .toList();
    }

    private TickerDocumentDto mapLatestSummaryToDto(com.oraculum.company.domain.TickerDocumentViewEntity entity) {
        return TickerDocumentDto.builder()
                .id(entity.getId())
                .ticker(entity.getTicker())
                .market(entity.getMarket())
                .documentType(entity.getDocumentType())
                .documentSubtype(entity.getDocumentSubtype())
                .reportPeriod(entity.getReportPeriod())
                .filingDate(entity.getFilingDate())
                .sourceUrl(entity.getSourceUrl())
                .accessionNumber(entity.getAccessionNumber())
                .summary(entity.getSummary())
                .sentimentScore(entity.getSentimentScore())
                .build();
    }
}

