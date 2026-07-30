package com.oraculum.harvester.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SecCikMapperClient {
    private final RestClient restClient;

    public SecCikMapperClient(@Qualifier("secEdgarRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Map<String, String> loadTickerToCikMapping() {
        log.info("Downloading SEC Ticker to CIK mapping from company_tickers.json...");
        try {
            Map<String, SecTickerRecord> rawMap = fetchRawTickerMapping();
            return buildCacheFromRawMap(rawMap);
        } catch (Exception e) {
            log.error("Failed to execute request to SEC EDGAR: {}", e.getMessage());
            return new ConcurrentHashMap<>();
        }
    }

    private Map<String, SecTickerRecord> fetchRawTickerMapping() {
        return restClient.get()
                .uri("/files/company_tickers.json")
                .exchange((_, response) -> {
                    Map<String, SecTickerRecord> rawMap = handleExchangeResponse(response);
                    if (rawMap == null || rawMap.isEmpty()) {
                        return Map.of();
                    }
                    return rawMap;
                });
    }

    private Map<String, SecTickerRecord> handleExchangeResponse(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws java.io.IOException {
        HttpStatusCode status = response.getStatusCode();

        if (status.isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE)) {
            log.warn("SEC EDGAR is temporarily unavailable (503). Skipping Ticker to CIK mapping.");
            return null;
        }
        if (status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
            log.warn("Rate limited by SEC EDGAR (429). Skipping Ticker to CIK mapping.");
            return null;
        }
        if (status.isError()) {
            log.error("Failed to fetch SEC Ticker to CIK mapping: {}", status);
            return null;
        }

        return response.bodyTo(new ParameterizedTypeReference<>() {
        });
    }

    private Map<String, String> buildCacheFromRawMap(Map<String, SecTickerRecord> rawMap) {
        Map<String, String> cache = new ConcurrentHashMap<>();
        rawMap.values().forEach(record -> {
            if (record.ticker() != null && record.cik_str() != null) {
                String paddedCik = String.format("%010d", record.cik_str());
                cache.put(record.ticker().toUpperCase(), paddedCik);
            }
        });
        log.info("Successfully loaded SEC Ticker to CIK mapping. Loaded {} records.", cache.size());
        return cache;
    }

    private record SecTickerRecord(Integer cik_str, String ticker, String title) {
    }
}
