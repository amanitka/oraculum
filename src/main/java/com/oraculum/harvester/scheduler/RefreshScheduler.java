package com.oraculum.harvester.scheduler;

import com.oraculum.harvester.service.NewsService;
import com.oraculum.harvester.service.RequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oraculum.data.refresh-enabled", havingValue = "true", matchIfMissing = true)
public class RefreshScheduler {

    private final RequestService refreshService;
    private final NewsService newsService;

    @Scheduled(cron = "${oraculum.data.metadata.cron}")
    public void refreshMetadata() {
        log.info("Starting scheduled monthly metadata refresh...");
        try {
            refreshService.refreshMarket();
            refreshService.refreshIndustry();
        } catch (Exception e) {
            log.error("Scheduled metadata refresh failed", e);
        }
    }

    @Scheduled(cron = "${oraculum.data.company.cron}")
    public void refreshCompany() {
        log.info("Starting scheduled company refresh...");
        try {
            refreshService.refreshCompany();
        } catch (Exception e) {
            log.error("Scheduled company refresh failed", e);
        }
    }

    @Scheduled(cron = "${oraculum.data.fundamentals.cron}")
    public void refreshFundamentals() {
        log.info("Starting scheduled fundamentals refresh...");
        try {
            refreshService.refreshFundamentals();
        } catch (Exception e) {
            log.error("Scheduled fundamentals refresh failed", e);
        }
    }

    @Scheduled(cron = "${oraculum.data.share-price.cron}")
    public void refreshSharePrices() {
        log.info("Starting scheduled daily share price refresh...");
        try {
            refreshService.refreshSharePrices();
        } catch (Exception e) {
            log.error("Scheduled share price refresh failed", e);
        }
    }

    @Scheduled(cron = "${oraculum.data.insider-transactions.cron}")
    public void refreshInsiderTransactions() {
        log.info("Starting scheduled insider transactions refresh...");
        try {
            refreshService.refreshInsiderTransactions();
        } catch (Exception e) {
            log.error("Scheduled insider transactions refresh failed", e);
        }
    }

    @Scheduled(cron = "${oraculum.data.news.cron}")
    public void refreshNews() {
        log.info("Starting scheduled news & sentiment refresh...");
        try {
            newsService.refreshNews();
        } catch (Exception e) {
            log.error("Scheduled news refresh failed", e);
        }
    }
}
