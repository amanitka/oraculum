package com.oraculum.harvester.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SecCikMapperClient {
    private final RestClient restClient;
    private final SecCikMapperClient self;

    public SecCikMapperClient(@Qualifier("secEdgarRestClient") RestClient restClient,
                              @Lazy SecCikMapperClient self) {
        this.restClient = restClient;
        this.self = self;
    }

    public String getCikForTicker(String ticker) {
        Map<String, String> cache = self.loadTickerToCikMapping();
        return cache != null ? cache.get(ticker.toUpperCase()) : null;
    }

    @Cacheable("secTickersCache")
    public Map<String, String> loadTickerToCikMapping() {
        log.info("Downloading SEC Ticker to CIK mapping from company_tickers.json...");
        try {
            Map<String, SecTickerRecord> rawMap = fetchRawTickerMapping();
            return buildCacheFromRawMap(rawMap);
        } catch (Exception e) {
            log.error("Failed to load SEC Ticker to CIK mapping", e);
            return new ConcurrentHashMap<>();
        }
    }

    private Map<String, SecTickerRecord> fetchRawTickerMapping() {
        Map<String, SecTickerRecord> rawMap = restClient.get()
                .uri("/files/company_tickers.json")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (rawMap == null || rawMap.isEmpty()) {
            log.warn("Received empty response from SEC company_tickers.json");
            return Map.of();
        }
        return rawMap;
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

    @CacheEvict(value = "secTickersCache", allEntries = true)
    @Scheduled(cron = "0 0 4 * * *") // Clear cache daily at 4 AM to force reload
    public void clearCache() {
        log.info("Evicting SEC Ticker to CIK mapping cache...");
    }

    private record SecTickerRecord(Integer cik_str, String ticker, String title) {
    }
}
