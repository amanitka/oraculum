package com.oraculum.harvester.service;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.harvester.api.dto.CompanyNewsDto;
import com.oraculum.harvester.provider.FinnhubClient;
import com.oraculum.harvester.provider.dto.FinnhubCompanyNewsItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinnhubNewsServiceTest {

    @Mock
    private FinnhubClient finnhubClient;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private OraculumProperties properties;

    private FinnhubNewsService service;

    @BeforeEach
    void setUp() {
        when(properties.harvester().finnhub().hasApiKey()).thenReturn(true);
        service = new FinnhubNewsService(finnhubClient, properties);
    }

    @Test
    void fetchCompanyNews_whenNotConfigured_returnsEmptyList() {
        when(properties.harvester().finnhub().hasApiKey()).thenReturn(false);
        FinnhubNewsService unconfigured = new FinnhubNewsService(finnhubClient, properties);

        List<CompanyNewsDto> result = unconfigured.fetchCompanyNews("AAPL", LocalDate.now().minusDays(7), LocalDate.now());

        assertThat(result).isEmpty();
        verifyNoInteractions(finnhubClient);
    }

    @Test
    void fetchCompanyNews_whenInvalidArguments_returnsEmptyList() {
        assertThat(service.fetchCompanyNews("", LocalDate.now(), LocalDate.now())).isEmpty();
        assertThat(service.fetchCompanyNews("AAPL", null, LocalDate.now())).isEmpty();
        assertThat(service.fetchCompanyNews("AAPL", LocalDate.now(), null)).isEmpty();
        verifyNoInteractions(finnhubClient);
    }

    @Test
    void fetchCompanyNews_filtersBotsDeduplicatesAndSortsNewestFirst() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 10);
        String longSummary = "A".repeat(500);

        FinnhubCompanyNewsItem older = new FinnhubCompanyNewsItem(
                "company news", 1767225600L, "Apple Unveils New Product", 1L, null, "AAPL", "Reuters", "Summary 1", "https://url1"
        );
        FinnhubCompanyNewsItem duplicateOlder = new FinnhubCompanyNewsItem(
                "company news", 1767225700L, "apple unveils new product ", 2L, null, "AAPL", "Yahoo", "Duplicate summary", "https://url2"
        );
        FinnhubCompanyNewsItem botArticle = new FinnhubCompanyNewsItem(
                "company news", 1767225800L, "Pension Fund Purchases 15,000 Shares of AAPL", 3L, null, "AAPL", "MarketBeat", "Bot summary", "https://url3"
        );
        FinnhubCompanyNewsItem blankHeadline = new FinnhubCompanyNewsItem(
                "company news", 1767225900L, "   ", 4L, null, "AAPL", "Source", "Summary", "https://url4"
        );
        FinnhubCompanyNewsItem newer = new FinnhubCompanyNewsItem(
                "company news", 1767226000L, "Analysts Raise Target for AAPL", 5L, null, "AAPL,MSFT", "Bloomberg", longSummary, "https://url5"
        );

        when(finnhubClient.fetchCompanyNews("AAPL", from, to))
                .thenReturn(List.of(older, duplicateOlder, botArticle, blankHeadline, newer));

        List<CompanyNewsDto> result = service.fetchCompanyNews("AAPL", from, to);

        assertThat(result).hasSize(2);
        // Newer timestamp (1767226000L vs 1767225600L) comes first
        assertThat(result.getFirst().headline()).isEqualTo("Analysts Raise Target for AAPL");
        assertThat(result.getFirst().related()).containsExactly("AAPL", "MSFT");
        assertThat(result.getFirst().summary()).hasSize(403);
        assertThat(result.getFirst().summary()).endsWith("...");

        assertThat(result.get(1).headline()).isEqualTo("Apple Unveils New Product");
    }

    @Test
    void fetchCompanyNews_whenExceptionThrown_returnsEmptyListGracefully() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 10);

        when(finnhubClient.fetchCompanyNews("AAPL", from, to))
                .thenThrow(new RuntimeException("API error"));

        List<CompanyNewsDto> result = service.fetchCompanyNews("AAPL", from, to);

        assertThat(result).isEmpty();
    }
}
