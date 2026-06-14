package com.oraculum.harvester.service;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.company.api.CompanyApi;
import com.oraculum.harvester.api.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HarvesterRequestServiceTest {

    private final String TOPIC = "harvester-topic";
    @Mock
    private CompanyApi companyApi;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private OraculumProperties properties;
    @Captor
    private ArgumentCaptor<HarvesterRequest> requestCaptor;
    private HarvesterRequestService harvesterRequestService;

    @BeforeEach
    void setUp() {
        when(properties.kafka().topics().harvesterRequest()).thenReturn(TOPIC);
        when(properties.data().sharePrice().incrementalWindowDays()).thenReturn(5);
        when(properties.data().news().incrementalWindowHours()).thenReturn(24);

        harvesterRequestService = new HarvesterRequestService(companyApi, kafkaTemplate, properties);
    }

    @Test
    void refreshMarket_publishesFetchMarketRequest() {
        harvesterRequestService.refreshMarket();

        verify(kafkaTemplate).send(eq(TOPIC), anyString(), requestCaptor.capture());
        assertThat(requestCaptor.getValue()).isInstanceOf(FetchMarketRequest.class);
    }

    @Test
    void refreshCompany_publishesForEveryMarket() {
        when(companyApi.getAllMarketIds()).thenReturn(List.of("US", "EU"));

        harvesterRequestService.refreshCompany();

        verify(kafkaTemplate, times(2)).send(eq(TOPIC), anyString(), requestCaptor.capture());
        List<HarvesterRequest> captured = requestCaptor.getAllValues();

        assertThat(captured).hasSize(2);
        assertThat(captured.get(0)).isInstanceOf(FetchCompanyRequest.class);
        assertThat(((FetchCompanyRequest) captured.get(0)).getMarket()).isEqualTo("US");
        assertThat(((FetchCompanyRequest) captured.get(1)).getMarket()).isEqualTo("EU");
    }

    @Test
    void refreshSharePrices_incremental_calculatesFromDateCorrectly() {
        when(companyApi.getAllMarketIds()).thenReturn(List.of("US"));
        LocalDate lastTradeDate = LocalDate.of(2023, 1, 10);
        when(companyApi.getSharePricesLastTradeDate()).thenReturn(Optional.of(lastTradeDate));

        // Incremental without explicit date
        harvesterRequestService.refreshSharePrices(true, null);

        verify(kafkaTemplate).send(eq(TOPIC), anyString(), requestCaptor.capture());
        FetchSharePricePriceRequest request = (FetchSharePricePriceRequest) requestCaptor.getValue();

        // 5 days window
        assertThat(request.getFromDate()).isEqualTo("2023-01-05");
        assertThat(request.getMarket()).isEqualTo("US");
    }

    @Test
    void refreshNews_incremental_calculatesTimeFromCorrectly() {
        OffsetDateTime lastNewsTime = OffsetDateTime.parse("2023-01-02T10:00:00Z");
        when(companyApi.getNewsLastTimePublished()).thenReturn(Optional.of(lastNewsTime));

        harvesterRequestService.refreshNews();

        verify(kafkaTemplate).send(eq(TOPIC), anyString(), requestCaptor.capture());
        FetchNewsRequest request = (FetchNewsRequest) requestCaptor.getValue();

        // 24 hours window, so 2023-01-01T10:00:00Z formatted compactly
        assertThat(request.getTimeFrom()).isEqualTo("20230101T1000");
        assertThat(request.getTimeFrom()).isNotNull();
    }
}
