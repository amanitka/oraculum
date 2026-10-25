package com.oraculum.harvester.provider;

import com.oraculum.harvester.provider.dto.AlphaVantageEarningsEstimatesResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class AlphaVantageClient {

    private final RestClient restClient;

    public AlphaVantageClient(@Qualifier("alphaVantageRestClient") RestClient restClient) {
        this.restClient = restClient;
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
