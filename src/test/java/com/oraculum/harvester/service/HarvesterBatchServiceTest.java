package com.oraculum.harvester.service;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.company.api.CompanyInsiderTransactionApi;
import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.CompanySecApi;
import com.oraculum.company.api.CompanySharePriceApi;
import com.oraculum.harvester.api.HarvesterBatchApi;
import com.oraculum.harvester.api.dto.Fetch13FBulkRequest;
import com.oraculum.harvester.api.dto.FetchCompanyRequest;
import com.oraculum.harvester.api.dto.FetchInsiderTransactionsRequest;
import com.oraculum.harvester.api.dto.FetchMarketRequest;
import com.oraculum.harvester.api.dto.FetchSecDocumentsRequest;
import com.oraculum.harvester.api.dto.FetchSharePricePriceRequest;
import com.oraculum.harvester.api.dto.HarvesterRequest;
import com.oraculum.harvester.api.event.FetchCompanyCusipsRequestEvent;
import com.oraculum.harvester.event.FetchMacroeconomicRequestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HarvesterBatchServiceTest {

    private final String TOPIC = "harvester-topic";
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private OraculumProperties properties;
    @Captor
    private ArgumentCaptor<HarvesterRequest> requestCaptor;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private CompanyMetadataApi companyMetadataApi;
    @Mock
    private CompanySharePriceApi companySharePriceApi;
    @Mock
    private CompanyInsiderTransactionApi companyInsiderTransactionApi;
    @Mock
    private CompanySecApi companySecApi;
    @Mock
    private SecDocumentHarvesterService secDocumentHarvesterService;
    private HarvesterBatchService harvesterBatchService;

    @BeforeEach
    void setUp() {
        when(properties.kafka().topics().harvesterRequest()).thenReturn(TOPIC);
        when(properties.data().sharePrice().incrementalWindowDays()).thenReturn(5);

        harvesterBatchService = new HarvesterBatchService(
                companyMetadataApi,
                companyInsiderTransactionApi,
                companySharePriceApi,
                companySecApi,
                secDocumentHarvesterService,
                kafkaTemplate,
                properties,
                eventPublisher);
    }

    @Test
    void refreshMarket_publishesFetchMarketRequest() {
        harvesterBatchService.refreshMarket();

        verify(kafkaTemplate).send(eq(TOPIC), anyString(), requestCaptor.capture());
        assertThat(requestCaptor.getValue()).isInstanceOf(FetchMarketRequest.class);
    }

    @Test
    void refreshCompany_publishesForEveryMarket() {
        when(companyMetadataApi.getAllMarketIds()).thenReturn(List.of("US", "EU"));

        harvesterBatchService.refreshCompany();

        verify(kafkaTemplate, times(2)).send(eq(TOPIC), anyString(), requestCaptor.capture());
        List<HarvesterRequest> captured = requestCaptor.getAllValues();

        assertThat(captured).hasSize(2);
        assertThat(captured.get(0)).isInstanceOf(FetchCompanyRequest.class);
        assertThat(((FetchCompanyRequest) captured.get(0)).getMarket()).isEqualTo("US");
        assertThat(((FetchCompanyRequest) captured.get(1)).getMarket()).isEqualTo("EU");
    }

    @Test
    void refreshSharePrices_incremental_calculatesFromDateCorrectly() {
        when(companyMetadataApi.getAllMarketIds()).thenReturn(List.of("US"));
        LocalDate lastTradeDate = LocalDate.of(2023, 1, 10);
        when(companySharePriceApi.getSharePricesLastTradeDate()).thenReturn(Optional.of(lastTradeDate));

        // Incremental without explicit date
        harvesterBatchService.refreshSharePrices(true, null);

        verify(kafkaTemplate).send(eq(TOPIC), anyString(), requestCaptor.capture());
        FetchSharePricePriceRequest request = (FetchSharePricePriceRequest) requestCaptor.getValue();

        // 5 days window
        assertThat(request.getFromDate()).isEqualTo("2023-01-05");
        assertThat(request.getMarket()).isEqualTo("US");
    }

    @Test
    void refreshInsiderTransactions_publishesFetchInsiderTransactionsRequest_withMaxDate() {
        LocalDateTime maxDate = LocalDateTime.of(2023, 10, 15, 14, 30, 0);
        when(companyInsiderTransactionApi.getInsiderTransactionsLastFilingDate()).thenReturn(Optional.of(maxDate));

        harvesterBatchService.refreshInsiderTransactions();

        verify(kafkaTemplate).send(eq(TOPIC), anyString(), requestCaptor.capture());
        HarvesterRequest request = requestCaptor.getValue();
        assertThat(request).isInstanceOf(FetchInsiderTransactionsRequest.class);

        FetchInsiderTransactionsRequest insiderRequest = (FetchInsiderTransactionsRequest) request;
        assertThat(insiderRequest.getRequestType()).isEqualTo("fetch_insider_transactions");
    }

    @Test
    void refreshInsiderTransactions_publishesFetchInsiderTransactionsRequest_withNullMaxDate() {
        when(companyInsiderTransactionApi.getInsiderTransactionsLastFilingDate()).thenReturn(Optional.empty());

        harvesterBatchService.refreshInsiderTransactions();

        verify(kafkaTemplate).send(eq(TOPIC), anyString(), requestCaptor.capture());
        assertThat(requestCaptor.getValue()).isInstanceOf(FetchInsiderTransactionsRequest.class);
    }

    @Test
    void refreshMacroeconomic_publishesMacroeconomicRefreshRequestedEvent() {
        harvesterBatchService.refreshMacroeconomic();
        verify(eventPublisher).publishEvent(any(FetchMacroeconomicRequestEvent.class));
    }

    @Test
    void refreshStaleSecDocuments_publishesFetchSecDocumentsRequest() {
        FetchSecDocumentsRequest requestPayload = FetchSecDocumentsRequest.builder()
                .items(List.of(
                        FetchSecDocumentsRequest.TickerDocumentItem.builder()
                                .ticker("AAPL")
                                .market("US")
                                .documentTypes(List.of(
                                        FetchSecDocumentsRequest.DocumentTypeRequest.builder()
                                                .documentType("8K")
                                                .lastProcessedFileDate(LocalDate.of(2023, 1, 1))
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(secDocumentHarvesterService.buildStaleSecDocumentsRequests()).thenReturn(List.of(requestPayload));

        harvesterBatchService.refreshStaleSecDocuments();

        verify(kafkaTemplate).send(eq(TOPIC), anyString(), requestCaptor.capture());
        HarvesterRequest request = requestCaptor.getValue();
        assertThat(request).isInstanceOf(FetchSecDocumentsRequest.class);

        FetchSecDocumentsRequest secRequest = (FetchSecDocumentsRequest) request;
        assertThat(secRequest.getItems()).hasSize(1);
        assertThat(secRequest.getItems().getFirst().getTicker()).isEqualTo("AAPL");
    }

    @Test
    void refreshCompanyCusips_publishesFetchCompanyCusipsRequestEvent() {
        harvesterBatchService.refreshCompanyCusips();

        verify(eventPublisher).publishEvent(any(FetchCompanyCusipsRequestEvent.class));
    }

    @ParameterizedTest
    @CsvSource({
            "2026-01-15, 2025-09-30",
            "2026-02-20, 2025-09-30",
            "2026-03-01, 2025-12-31",
            "2026-05-31, 2025-12-31",
            "2026-06-01, 2026-03-31",
            "2026-08-31, 2026-03-31",
            "2026-09-01, 2026-06-30",
            "2026-11-13, 2026-06-30",
            "2026-12-01, 2026-09-30",
            "2026-12-31, 2026-09-30"
    })
    void resolveLatestAvailable13FPeriodEnd_whenGivenDate_returnsExpectedQuarterEnd(LocalDate asOfDate,
                                                                                    LocalDate expectedPeriodEnd) {
        assertThat(HarvesterBatchApi.resolveLatestAvailable13FPeriodEnd(asOfDate)).isEqualTo(expectedPeriodEnd);
    }

    @Test
    void refresh13FBulkIfMissing_whenPeriodNotLoaded_publishesFetch13FBulkRequest() {
        LocalDate asOfDate = LocalDate.of(2026, 12, 4);
        LocalDate expectedPeriod = LocalDate.of(2026, 9, 30);
        when(companySecApi.hasHoldingsForPeriod(expectedPeriod)).thenReturn(false);

        harvesterBatchService.refresh13FBulkIfMissing(asOfDate);

        verify(kafkaTemplate).send(eq(TOPIC), anyString(), requestCaptor.capture());
        assertThat(requestCaptor.getValue()).isInstanceOf(Fetch13FBulkRequest.class);
        Fetch13FBulkRequest request = (Fetch13FBulkRequest) requestCaptor.getValue();
        assertThat(request.getYear()).isEqualTo(2026);
        assertThat(request.getQuarter()).isEqualTo(3);
    }

    @Test
    void refresh13FBulkIfMissing_whenPeriodAlreadyLoaded_skipsPublish() {
        LocalDate asOfDate = LocalDate.of(2026, 12, 5);
        LocalDate expectedPeriod = LocalDate.of(2026, 9, 30);
        when(companySecApi.hasHoldingsForPeriod(expectedPeriod)).thenReturn(true);

        harvesterBatchService.refresh13FBulkIfMissing(asOfDate);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}
