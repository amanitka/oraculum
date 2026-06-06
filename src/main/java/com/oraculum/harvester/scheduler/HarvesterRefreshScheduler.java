package com.oraculum.harvester.scheduler;

import com.oraculum.company.api.CompanyApi;
import com.oraculum.company.api.domain.StatementTemplate;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.MarketDto;
import com.oraculum.harvester.api.dto.*;
import com.oraculum.harvester.service.HarvesterRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oraculum.jobs.data-refresh.enabled", havingValue = "true", matchIfMissing = true)
public class HarvesterRefreshScheduler {

    private final HarvesterRequestService refreshService;
    private final CompanyApi companyApi;

    private void refreshFundamentals(String market) {
        List<String> templates = Stream.of(StatementTemplate.values()).map(StatementTemplate::getValue).toList();
        for (StatementVariant variant : StatementVariant.values()) {
            log.info("Requesting income statement refresh for market: {}, variant: {}", market, variant);
            refreshService.publish(new FetchIncomeStatementRequest(market, variant.getValue(), templates));

            log.info("Requesting balance sheet refresh for market: {}, variant: {}", market, variant);
            refreshService.publish(new FetchBalanceSheetRequest(market, variant.getValue(), templates));

            log.info("Requesting cash flow statement refresh for market: {}, variant: {}", market, variant);
            refreshService.publish(new FetchCashFlowStatementRequest(market, variant.getValue(), templates));
        }
    }

    @Scheduled(cron = "${oraculum.jobs.data-refresh.share-price-cron:0 0 3 * * TUE-SAT}")
    public void refreshSharePrices() {
        log.info("Starting scheduled daily share price refresh...");
        try {
            List<String> markets = companyApi.getAllMarkets().stream().map(MarketDto::marketId).toList();
            for (String market : markets) {
                log.info("Requesting share price refresh for market: {}", market);
                String fromDate = LocalDate.now().toString();
                HarvesterRequest request = new FetchSharePricePriceRequest(market, "daily", fromDate, 7);
                refreshService.publish(request);
            }
        } catch (Exception e) {
            log.error("Scheduled share price refresh failed", e);
        }
    }

    @Scheduled(cron = "${oraculum.jobs.data-refresh.news-cron:0 0 */4 * * *}")
    public void refreshNews() {
        log.info("Starting scheduled news & sentiment refresh...");
        try {
            OffsetDateTime lastTime = companyApi.getNewsLastTimePublished().orElseGet(() -> OffsetDateTime.now().minusDays(1));
            OffsetDateTime startTime = lastTime.minusHours(1);
            String timeFrom = startTime.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm"));

            log.info("Requesting incremental news refresh from: {}", timeFrom);
            HarvesterRequest request = FetchNewsRequest.builder().timeFrom(timeFrom).build();
            refreshService.publish(request);
        } catch (Exception e) {
            log.error("Scheduled news refresh failed", e);
        }
    }

    @Scheduled(cron = "${oraculum.jobs.data-refresh.fundamentals-cron:0 0 2 * * TUE}")
    public void refreshFundamentals() {
        log.info("Starting scheduled weekly fundamentals refresh...");
        try {
            List<String> markets = companyApi.getAllMarkets().stream().map(MarketDto::marketId).toList();
            for (var market : markets) {
                log.info("Requesting company list refresh for market: {}", market);
                refreshService.publish(FetchCompanyRequest.builder().market(market).build());
                log.info("Requesting fundamentals refresh for market: {}", market);
                refreshFundamentals(market);
            }
        } catch (Exception e) {
            log.error("Scheduled fundamentals refresh failed", e);
        }
    }

    @Scheduled(cron = "${oraculum.jobs.data-refresh.metadata-cron:0 0 1 1 * *}")
    public void refreshMetadata() {
        log.info("Starting scheduled monthly metadata refresh...");
        try {
            log.info("Requesting market metadata refresh");
            refreshService.publish(FetchMarketRequest.builder().build());
            log.info("Requesting industry metadata refresh");
            refreshService.publish(FetchIndustryRequest.builder().build());
        } catch (Exception e) {
            log.error("Scheduled metadata refresh failed", e);
        }
    }
}
