package com.oraculum.company.service.impl;

import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.TickerDocumentSyncStatusDto;
import com.oraculum.company.domain.TickerDocumentSyncStatusEntity;
import com.oraculum.company.domain.TickerSecDocumentStaleSyncEntity;
import com.oraculum.company.repository.TickerDocumentSyncStatusRepository;
import com.oraculum.company.repository.TickerSecDocumentStaleSyncRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyTickerDocumentServiceImplTest {

    @Mock
    private TickerDocumentSyncStatusRepository repository;

    @Mock
    private TickerSecDocumentStaleSyncRepository staleSyncRepository;

    @InjectMocks
    private CompanyTickerDocumentServiceImpl service;

    @Test
    void getSyncStatusesByTickersAndMarket_returnsMappedDtos() {
        TickerDocumentSyncStatusEntity entity = TickerDocumentSyncStatusEntity.builder()
                .ticker("AAPL")
                .market("US")
                .documentType("8K")
                .lastProcessedFileDate(LocalDate.of(2023, 1, 1))
                .build();

        when(repository.findByTickerInAndMarket(List.of("AAPL"), "US")).thenReturn(List.of(entity));

        List<TickerDocumentSyncStatusDto> result = service.getSyncStatusesByTickersAndMarket(List.of("AAPL"), "US");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTicker()).isEqualTo("AAPL");
        assertThat(result.getFirst().getMarket()).isEqualTo("US");
        assertThat(result.getFirst().getDocumentType()).isEqualTo(TickerDocumentType.FORM_8K);
        assertThat(result.getFirst().getLastProcessedFileDate()).isEqualTo(LocalDate.of(2023, 1, 1));
    }

    @Test
    void getStaleSecDocuments_returnsMappedDtos() {
        TickerSecDocumentStaleSyncEntity entity = TickerSecDocumentStaleSyncEntity.builder()
                .ticker("MSFT")
                .market("US")
                .documentType("10K")
                .lastProcessedFileDate(LocalDate.of(2023, 6, 1))
                .build();

        when(staleSyncRepository.findStaleDocuments(PageRequest.of(0, 10))).thenReturn(List.of(entity));

        List<TickerDocumentSyncStatusDto> result = service.getStaleSecDocuments(10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTicker()).isEqualTo("MSFT");
        assertThat(result.getFirst().getMarket()).isEqualTo("US");
        assertThat(result.getFirst().getDocumentType()).isEqualTo(TickerDocumentType.FORM_10K);
        assertThat(result.getFirst().getLastProcessedFileDate()).isEqualTo(LocalDate.of(2023, 6, 1));
    }
}
