package com.oraculum.harvester.service;

import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.TickerDocumentSyncStatusDto;
import com.oraculum.harvester.api.dto.FetchSecDocumentsRequest;
import com.oraculum.company.api.dto.TickerKeyDto;
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
    private CompanyMetadataApi companyMetadataApi;

    @Mock
    private CompanyTickerDocumentApi companyTickerDocumentApi;

    @InjectMocks
    private SecDocumentHarvesterService service;

    @Test
    void buildStaleSecDocumentsRequests_emptyStaleDocs_returnsEmptyList() {
        when(companyTickerDocumentApi.getStaleSecDocuments(100)).thenReturn(List.of());

        List<FetchSecDocumentsRequest> requests = service.buildStaleSecDocumentsRequests();

        assertThat(requests).isEmpty();
    }

    @Test
    void buildStaleSecDocumentsRequests_withStaleDocs_groupsAndBuildsRequests() {
        TickerDocumentSyncStatusDto dto1 = TickerDocumentSyncStatusDto.builder()
                .ticker("AAPL")
                .market("US")
                .cik("0000320193")
                .documentType(TickerDocumentType.SEC_8K)
                .lastProcessedFileDate(LocalDate.of(2023, 1, 1))
                .build();
        TickerDocumentSyncStatusDto dto2 = TickerDocumentSyncStatusDto.builder()
                .ticker("AAPL")
                .market("US")
                .documentType(TickerDocumentType.SEC_10K)
                .lastProcessedFileDate(LocalDate.of(2023, 1, 10))
                .build();
        TickerDocumentSyncStatusDto dto3 = TickerDocumentSyncStatusDto.builder()
                .ticker("MSFT")
                .market("US")
                .documentType(TickerDocumentType.SEC_10K)
                .lastProcessedFileDate(null)
                .build();

        when(companyTickerDocumentApi.getStaleSecDocuments(100)).thenReturn(List.of(dto1, dto2, dto3));

        List<FetchSecDocumentsRequest> requests = service.buildStaleSecDocumentsRequests();

        assertThat(requests).hasSize(1);
        FetchSecDocumentsRequest secRequest = requests.getFirst();
        assertThat(secRequest.getItems()).hasSize(2); // AAPL and MSFT

        FetchSecDocumentsRequest.TickerDocumentItem aaplItem = secRequest.getItems().stream()
                .filter(item -> "AAPL".equals(item.getTicker()))
                .findFirst().orElseThrow();
        assertThat(aaplItem.getDocumentTypes()).hasSize(2);
        assertThat(aaplItem.getCik()).isEqualTo("0000320193");

        FetchSecDocumentsRequest.TickerDocumentItem msftItem = secRequest.getItems().stream()
                .filter(item -> "MSFT".equals(item.getTicker()))
                .findFirst().orElseThrow();
        assertThat(msftItem.getDocumentTypes()).hasSize(1);
        assertThat(msftItem.getDocumentTypes().getFirst().getLastProcessedFileDate()).isNull();
    }

    @Test
    void buildStaleSecDocumentsRequests_withMoreThan20Docs_chunksIntoBatchesOf20() {
        List<TickerDocumentSyncStatusDto> staleDocs = new java.util.ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            staleDocs.add(TickerDocumentSyncStatusDto.builder()
                    .ticker("TICK" + i)
                    .market("US")
                    .documentType(TickerDocumentType.SEC_8K)
                    .lastProcessedFileDate(LocalDate.of(2023, 1, 1))
                    .build());
        }

        when(companyTickerDocumentApi.getStaleSecDocuments(100)).thenReturn(staleDocs);

        List<FetchSecDocumentsRequest> requests = service.buildStaleSecDocumentsRequests();

        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).getItems()).hasSize(20);
        assertThat(requests.get(1).getItems()).hasSize(5);
    }

    @Test
    void buildSecDocumentsRequest_buildsRequestCorrectly() {
        CompanyDto company1 = new CompanyDto(1, "AAPL", "US", "Apple Inc.", null, null, null, null, null, null, null, "0000320193", null);
        when(companyMetadataApi.getCompaniesByMarketAndTickers("US", List.of("AAPL"))).thenReturn(List.of(company1));

        TickerDocumentSyncStatusDto status1 = TickerDocumentSyncStatusDto.builder()
                .ticker("AAPL")
                .market("US")
                .documentType(TickerDocumentType.SEC_8K)
                .lastProcessedFileDate(LocalDate.of(2023, 1, 1))
                .build();
        when(companyTickerDocumentApi.getSyncStatusesByTickersAndMarket(List.of("AAPL"), "US"))
                .thenReturn(List.of(status1));

        Optional<FetchSecDocumentsRequest> requestOpt = service.buildSecDocumentsRequest(
                List.of(new TickerKeyDto("AAPL", "US"))
        );

        assertThat(requestOpt).isPresent();
        FetchSecDocumentsRequest secRequest = requestOpt.get();
        assertThat(secRequest.getItems()).hasSize(1);
        assertThat(secRequest.getItems().getFirst().getTicker()).isEqualTo("AAPL");
        assertThat(secRequest.getItems().getFirst().getMarket()).isEqualTo("US");
        assertThat(secRequest.getItems().getFirst().getCik()).isEqualTo("0000320193");
        assertThat(secRequest.getItems().getFirst().getDocumentTypes()).hasSize(2);
    }
}
