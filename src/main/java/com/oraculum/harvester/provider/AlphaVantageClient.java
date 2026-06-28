package com.oraculum.harvester.provider;

import com.oraculum.harvester.provider.dto.AlphaVantageEarningsEstimatesResponse;
import com.oraculum.harvester.provider.dto.AlphaVantageNewsResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
@Slf4j
public class AlphaVantageClient {

    private final RestClient restClient;

    public AlphaVantageClient(@Qualifier("alphaVantageRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retry(name = "alphaVantageApi")
    public AlphaVantageNewsResponse fetchNewsSentiment(String timeFrom) {
        log.info("Fetching news sentiment from Alpha Vantage. TimeFrom: {}", timeFrom);
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/query")
                        .queryParam("function", "NEWS_SENTIMENT")
                        .queryParam("limit", 1000)
                        .queryParamIfPresent("time_from", Optional.ofNullable(timeFrom))
                        .build())
                .retrieve()
                .body(AlphaVantageNewsResponse.class);
    }

    @Retry(name = "alphaVantageApi")
    public AlphaVantageEarningsEstimatesResponse fetchEarningsEstimates(String symbol) {
        log.info("Fetching earnings estimates from Alpha Vantage for symbol: {}", symbol);
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/query")
                        .queryParam("function", "EARNINGS_ESTIMATES")
                        .queryParam("symbol", symbol)
                        .build())
                .retrieve()
                .body(AlphaVantageEarningsEstimatesResponse.class);
    }
}
