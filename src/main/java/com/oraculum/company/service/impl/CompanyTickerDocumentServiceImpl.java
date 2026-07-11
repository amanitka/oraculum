package com.oraculum.company.service.impl;

import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.dto.TickerDocumentSyncStatusDto;
import com.oraculum.company.domain.TickerDocumentSyncStatusEntity;
import com.oraculum.company.domain.TickerSecDocumentStaleSyncEntity;
import com.oraculum.company.repository.TickerDocumentSyncStatusRepository;
import com.oraculum.company.repository.TickerSecDocumentStaleSyncRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyTickerDocumentServiceImpl implements CompanyTickerDocumentApi {

    private final TickerDocumentSyncStatusRepository repository;
    private final TickerSecDocumentStaleSyncRepository staleSyncRepository;

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
}

