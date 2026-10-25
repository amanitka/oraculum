package com.oraculum.harvester.provider;

import com.oraculum.harvester.provider.dto.FinnhubCompanyNewsItem;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class FinnhubClient {

    private final RestClient restClient;

    public FinnhubClient(@Qualifier("finnhubRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retry(name = "finnhubApi")
    public List<FinnhubCompanyNewsItem> fetchCompanyNews(String symbol, LocalDate from, LocalDate to) {
        log.info("Fetching company news from Finnhub for symbol: {}, from: {}, to: {}", symbol, from, to);
        List<FinnhubCompanyNewsItem> items = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/company-news")
                        .queryParam("symbol", symbol)
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString())
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return items != null ? items : List.of();
    }
}
