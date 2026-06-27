package com.oraculum.harvester.service;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.company.api.CompanyInsiderTransactionApi;
import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.CompanySharePriceApi;
import com.oraculum.harvester.api.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HarvesterBatchServiceTest {

    private final String TOPIC = "harvester-topic";
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
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
    private HarvesterBatchService harvesterBatchService;

    @BeforeEach
    void setUp() {
        when(properties.kafka().topics().harvesterRequest()).thenReturn(TOPIC);
        when(properties.data().sharePrice().incrementalWindowDays()).thenReturn(5);

        harvesterBatchService = new HarvesterBatchService(companyMetadataApi, companyInsiderTransactionApi, companySharePriceApi, kafkaTemplate, properties, eventPublisher);
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
    void refreshNews_publishesNewsRefreshRequestedEvent() {
        harvesterBatchService.refreshNews();
        verify(eventPublisher).publishEvent(any(com.oraculum.harvester.event.FetchNewsRequestEvent.class));
    }

    @Test
    void refreshMacroeconomic_publishesMacroeconomicRefreshRequestedEvent() {
        harvesterBatchService.refreshMacroeconomic();
        verify(eventPublisher).publishEvent(any(com.oraculum.harvester.event.FetchMacroeconomicRequestEvent.class));
    }
}
