package com.oraculum.harvester.provider;

import com.oraculum.harvester.provider.dto.FredResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Component
public class FredClient {
    private final RestClient restClient;

    public FredClient(@Qualifier("fredRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retry(name = "fredApi")
    public FredResponse fetchMacroeconomicData(String seriesId, String dateFrom) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/series/observations")
                        .queryParam("series_id", seriesId)
                        .queryParam("file_type", "json")
                        .queryParamIfPresent("observation_start", Optional.ofNullable(dateFrom))
                        .build())
                .retrieve()
                .body(FredResponse.class);
    }

}

