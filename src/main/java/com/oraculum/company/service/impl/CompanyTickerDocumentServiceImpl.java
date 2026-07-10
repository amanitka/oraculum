package com.oraculum.company.service.impl;

import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.dto.TickerDocumentSyncStatusDto;
import com.oraculum.company.domain.TickerDocumentSyncStatusEntity;
import com.oraculum.company.repository.TickerDocumentSyncStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyTickerDocumentServiceImpl implements CompanyTickerDocumentApi {

    private final TickerDocumentSyncStatusRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<TickerDocumentSyncStatusDto> getSyncStatusesByTickersAndMarket(List<String> tickers, String market) {
        return repository.findByTickerInAndMarket(tickers, market).stream()
                .map(this::mapToDto)
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
}
