package com.oraculum.company.service.impl;

import com.oraculum.company.api.domain.TickerDocumentSubtype;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.TickerDocumentDto;
import com.oraculum.company.api.dto.TickerDocumentPendingDto;
import com.oraculum.company.api.dto.TickerDocumentSyncStatusDto;
import com.oraculum.company.domain.TickerDocumentEntity;
import com.oraculum.company.domain.TickerDocumentPendingEntity;
import com.oraculum.company.domain.TickerDocumentSyncStatusEntity;
import com.oraculum.company.domain.TickerSecDocumentStaleSyncEntity;
import com.oraculum.company.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyTickerDocumentServiceImplTest {

    @Mock
    private TickerDocumentSyncStatusRepository repository;

    @Mock
    private TickerSecDocumentStaleSyncRepository staleSyncRepository;

    @Mock
    private TickerDocumentPendingRepository pendingRepository;

    @Mock
    private TickerDocumentRawRepository rawRepository;

    @Mock
    private TickerDocumentRepository summaryRepository;

    @InjectMocks
    private CompanyTickerDocumentServiceImpl service;

    @Test
    void getSyncStatusesByTickersAndMarket_returnsMappedDtos() {
        TickerDocumentSyncStatusEntity entity = TickerDocumentSyncStatusEntity.builder()
                .ticker("AAPL")
                .market("US")
                .documentType("SEC_8K")
                .lastProcessedFileDate(LocalDate.of(2023, 1, 1))
                .build();

        when(repository.findByTickerInAndMarket(List.of("AAPL"), "US")).thenReturn(List.of(entity));

        List<TickerDocumentSyncStatusDto> result = service.getSyncStatusesByTickersAndMarket(List.of("AAPL"), "US");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTicker()).isEqualTo("AAPL");
        assertThat(result.getFirst().getMarket()).isEqualTo("US");
        assertThat(result.getFirst().getDocumentType()).isEqualTo(TickerDocumentType.SEC_8K);
        assertThat(result.getFirst().getLastProcessedFileDate()).isEqualTo(LocalDate.of(2023, 1, 1));
    }

    @Test
    void getStaleSecDocuments_returnsMappedDtos() {
        TickerSecDocumentStaleSyncEntity entity = TickerSecDocumentStaleSyncEntity.builder()
                .ticker("MSFT")
                .market("US")
                .documentType("SEC_10K")
                .lastProcessedFileDate(LocalDate.of(2023, 6, 1))
                .build();

        when(staleSyncRepository.findStaleDocuments(PageRequest.of(0, 10))).thenReturn(List.of(entity));

        List<TickerDocumentSyncStatusDto> result = service.getStaleSecDocuments(10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTicker()).isEqualTo("MSFT");
        assertThat(result.getFirst().getMarket()).isEqualTo("US");
        assertThat(result.getFirst().getDocumentType()).isEqualTo(TickerDocumentType.SEC_10K);
        assertThat(result.getFirst().getLastProcessedFileDate()).isEqualTo(LocalDate.of(2023, 6, 1));
    }

    @Test
    void getPendingRawDocuments_returnsMappedDtos() {
        TickerDocumentPendingEntity entity = TickerDocumentPendingEntity.builder()
                .id("hash123")
                .ticker("AAPL")
                .market("US")
                .documentType(TickerDocumentType.SEC_10K)
                .documentSubtype(TickerDocumentSubtype.SEC_MD)
                .reportPeriod(LocalDate.of(2023, 12, 31))
                .filingDate(LocalDate.of(2024, 2, 1))
                .content("Management discussion content")
                .companyName("Apple Inc.")
                .marketCapitalization(3000000000000.0)
                .companySize("LARGE")
                .build();

        when(pendingRepository.findPendingDocuments(PageRequest.of(0, 5))).thenReturn(List.of(entity));

        List<TickerDocumentPendingDto> result = service.getPendingRawDocuments(5);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo("hash123");
        assertThat(result.getFirst().getTicker()).isEqualTo("AAPL");
        assertThat(result.getFirst().getDocumentSubtype()).isEqualTo(TickerDocumentSubtype.SEC_MD);
    }

    @Test
    void createDocumentSummary_savesAndUpdatesRawStatus() {
        TickerDocumentDto dto = TickerDocumentDto.builder()
                .id("hash123")
                .ticker("AAPL")
                .market("US")
                .documentType(TickerDocumentType.SEC_10K)
                .documentSubtype(TickerDocumentSubtype.SEC_MD)
                .reportPeriod(LocalDate.of(2023, 12, 31))
                .summary("Parsed JSON summary")
                .sentimentScore(0.8f)
                .build();

        service.createDocumentSummary(dto);

        org.mockito.ArgumentCaptor<TickerDocumentEntity> entityCaptor = org.mockito.ArgumentCaptor.forClass(TickerDocumentEntity.class);
        verify(summaryRepository).save(entityCaptor.capture());
        verify(rawRepository).updateStatus("hash123", LocalDate.of(2023, 12, 31), "PROCESSED");

        TickerDocumentEntity saved = entityCaptor.getValue();
        assertThat(saved.getId()).isEqualTo("hash123");
        assertThat(saved.getSummary()).isEqualTo("Parsed JSON summary");
        assertThat(saved.getSentimentScore()).isEqualTo(0.8f);
    }

    @Test
    void updateRawDocumentStatus_callsRepository() {
        service.updateRawDocumentStatus("hash123", LocalDate.of(2023, 12, 31), "FAILED");

        verify(rawRepository).updateStatus("hash123", LocalDate.of(2023, 12, 31), "FAILED");
    }

    @Test
    void getPendingRawDocumentsByTicker_returnsMappedDtos() {
        TickerDocumentPendingEntity entity = TickerDocumentPendingEntity.builder()
                .id("hash123")
                .ticker("AAPL")
                .market("US")
                .documentType(TickerDocumentType.SEC_10K)
                .documentSubtype(TickerDocumentSubtype.SEC_MD)
                .reportPeriod(LocalDate.of(2023, 12, 31))
                .filingDate(LocalDate.of(2024, 2, 1))
                .content("Content details")
                .companyName("Apple Inc.")
                .marketCapitalization(3000000000000.0)
                .companySize("LARGE")
                .build();

        when(pendingRepository.findPendingByTickerAndMarket("AAPL", "US")).thenReturn(List.of(entity));

        List<TickerDocumentPendingDto> result = service.getPendingRawDocumentsByTicker("AAPL", "US");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo("hash123");
        assertThat(result.getFirst().getContent()).isEqualTo("Content details");
    }
}
