package com.oraculum.harvester.service;

import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.TickerDocumentSyncStatusDto;
import com.oraculum.harvester.api.dto.FetchSecDocumentsRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecDocumentHarvesterServiceTest {

    @Mock
    private CompanyTickerDocumentApi companyTickerDocumentApi;

    @InjectMocks
    private SecDocumentHarvesterService service;

    @Test
    void buildStaleSecDocumentsRequest_emptyStaleDocs_returnsEmptyOptional() {
        when(companyTickerDocumentApi.getStaleSecDocuments(200)).thenReturn(List.of());

        Optional<FetchSecDocumentsRequest> requestOpt = service.buildStaleSecDocumentsRequest();

        assertThat(requestOpt).isEmpty();
    }

    @Test
    void buildStaleSecDocumentsRequest_withStaleDocs_groupsAndBuildsRequest() {
        TickerDocumentSyncStatusDto dto1 = TickerDocumentSyncStatusDto.builder()
                .ticker("AAPL")
                .market("US")
                .documentType(TickerDocumentType.FORM_8K)
                .lastProcessedFileDate(LocalDate.of(2023, 1, 1))
                .build();
        TickerDocumentSyncStatusDto dto2 = TickerDocumentSyncStatusDto.builder()
                .ticker("AAPL")
                .market("US")
                .documentType(TickerDocumentType.FORM_10K)
                .lastProcessedFileDate(LocalDate.of(2023, 1, 10))
                .build();
        TickerDocumentSyncStatusDto dto3 = TickerDocumentSyncStatusDto.builder()
                .ticker("MSFT")
                .market("US")
                .documentType(TickerDocumentType.FORM_10K)
                .lastProcessedFileDate(null)
                .build();

        when(companyTickerDocumentApi.getStaleSecDocuments(200)).thenReturn(List.of(dto1, dto2, dto3));

        Optional<FetchSecDocumentsRequest> requestOpt = service.buildStaleSecDocumentsRequest();

        assertThat(requestOpt).isPresent();
        FetchSecDocumentsRequest secRequest = requestOpt.get();
        assertThat(secRequest.getItems()).hasSize(2); // AAPL and MSFT

        FetchSecDocumentsRequest.TickerDocumentItem aaplItem = secRequest.getItems().stream()
                .filter(item -> "AAPL".equals(item.getTicker()))
                .findFirst().orElseThrow();
        assertThat(aaplItem.getDocumentTypes()).hasSize(2);

        FetchSecDocumentsRequest.TickerDocumentItem msftItem = secRequest.getItems().stream()
                .filter(item -> "MSFT".equals(item.getTicker()))
                .findFirst().orElseThrow();
        assertThat(msftItem.getDocumentTypes()).hasSize(1);
        assertThat(msftItem.getDocumentTypes().getFirst().getLastProcessedFileDate()).isNull();
    }
}
