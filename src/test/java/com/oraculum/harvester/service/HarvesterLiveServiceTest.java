package com.oraculum.harvester.service;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.harvester.api.dto.CompanyNewsDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import com.oraculum.harvester.domain.ProviderType;
import com.oraculum.harvester.provider.AlphaVantageClient;
import com.oraculum.harvester.provider.dto.AlphaVantageEarningsEstimatesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HarvesterLiveServiceTest {

    @Mock
    private AlphaVantageClient alphaVantageClient;

    @Mock
    private ApiUsageTrackerService apiUsageTrackerService;

    @Mock
    private FinnhubNewsService finnhubNewsService;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private OraculumProperties properties;

    private HarvesterLiveService service;

    @BeforeEach
    void setUp() {
        when(properties.harvester().alphaVantage().dailyLimit()).thenReturn(25);
        when(properties.harvester().alphaVantage().reservedCalls()).thenReturn(0);
        service = new HarvesterLiveService(alphaVantageClient, apiUsageTrackerService, finnhubNewsService, properties);
    }

    @Test
    void fetchEarningsEstimates_whenQuotaExceeded_returnsEmpty() {
        when(apiUsageTrackerService.canMakeCall(eq(ProviderType.ALPHA_VANTAGE), anyInt())).thenReturn(false);

        Optional<List<EarningsEstimateDto>> result = service.fetchEarningsEstimates("AAPL");

        assertThat(result).isEmpty();
        verifyNoInteractions(alphaVantageClient);
    }

    @Test
    void fetchEarningsEstimates_whenSuccessful_recordsCallAndReturnsEstimates() {
        when(apiUsageTrackerService.canMakeCall(eq(ProviderType.ALPHA_VANTAGE), anyInt())).thenReturn(true);
        EarningsEstimateDto dto = mock(EarningsEstimateDto.class);
        AlphaVantageEarningsEstimatesResponse response = new AlphaVantageEarningsEstimatesResponse("AAPL", List.of(dto));
        when(alphaVantageClient.fetchEarningsEstimates("AAPL")).thenReturn(response);

        Optional<List<EarningsEstimateDto>> result = service.fetchEarningsEstimates("AAPL");

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(dto);
        verify(apiUsageTrackerService).recordCall(ProviderType.ALPHA_VANTAGE);
    }

    @Test
    void fetchCompanyNews_delegatesToFinnhubNewsService() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 10);
        CompanyNewsDto item1 = new CompanyNewsDto(1L, OffsetDateTime.now(), "Head 1", "Sum 1", "Src", "url1", List.of());
        CompanyNewsDto item2 = new CompanyNewsDto(2L, OffsetDateTime.now(), "Head 2", "Sum 2", "Src", "url2", List.of());

        when(finnhubNewsService.fetchCompanyNews("AAPL", from, to)).thenReturn(List.of(item1, item2));

        List<CompanyNewsDto> all = service.fetchCompanyNews("AAPL", from, to);
        assertThat(all).containsExactly(item1, item2);

        List<CompanyNewsDto> limited = service.fetchCompanyNews("AAPL", from, to, 1);
        assertThat(limited).containsExactly(item1);
    }
}
