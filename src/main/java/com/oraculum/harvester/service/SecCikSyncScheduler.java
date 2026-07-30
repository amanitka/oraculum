package com.oraculum.harvester.service;

import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.harvester.provider.SecCikMapperClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecCikSyncScheduler {

    private final SecCikMapperClient secCikMapperClient;
    private final CompanyMetadataApi companyMetadataApi;

    // Run daily at 4:00 AM
    @Scheduled(cron = "0 0 4 * * *")
    public void syncCompanyCiks() {
        log.info("Starting scheduled SEC CIK synchronization...");
        try {
            Map<String, String> tickerToCikMap = secCikMapperClient.loadTickerToCikMapping();
            if (tickerToCikMap != null && !tickerToCikMap.isEmpty()) {
                companyMetadataApi.updateCompanyCiks(tickerToCikMap);
                log.info("Successfully completed SEC CIK synchronization.");
            } else {
                log.warn("SEC CIK map is empty. Skipping synchronization.");
            }
        } catch (Exception e) {
            log.error("Failed to synchronize SEC CIKs", e);
        }
    }
}
