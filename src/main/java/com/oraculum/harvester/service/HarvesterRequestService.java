package com.oraculum.harvester.service;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.company.api.CompanyApi;
import com.oraculum.company.api.domain.StatementTemplate;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.harvester.api.HarvesterRequestApi;
import com.oraculum.harvester.api.dto.*;
import com.oraculum.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
public class HarvesterRequestService implements HarvesterRequestApi {

    private final CompanyApi companyApi;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String harvesterRequestTopic;
    private final int sharePriceIncrementalWindowDays;
    private final int newsIncrementalWindowHours;

    public HarvesterRequestService(CompanyApi companyApi, KafkaTemplate<String, Object> kafkaTemplate, OraculumProperties properties) {
        this.companyApi = companyApi;
        this.kafkaTemplate = kafkaTemplate;
        this.harvesterRequestTopic = properties.kafka().topics().harvesterRequest();
        this.sharePriceIncrementalWindowDays = properties.data().sharePrice().incrementalWindowDays();
        this.newsIncrementalWindowHours = properties.data().news().incrementalWindowHours();
    }

    private List<String> getMarkets() {
        return companyApi.getAllMarketIds();
    }

    private void publishRequest(HarvesterRequest request) {
        try {
            kafkaTemplate.send(harvesterRequestTopic, request.getCorrelationId().toString(), request);
            log.info("Published {} [{}] to {}", request.getRequestType(), request.getCorrelationId(), harvesterRequestTopic);
        } catch (Exception e) {
            log.error("Failed to publish {} request", request.getRequestType(), e);
            throw new RuntimeException("Failed to publish request: " + e.getMessage(), e);
        }
    }

    @Override
    public void refreshMarket() {
        log.info("Requesting market data refresh");
        publishRequest(FetchMarketRequest.builder().build());
    }

    @Override
    public void refreshIndustry() {
        log.info("Requesting industry data refresh");
        publishRequest(FetchIndustryRequest.builder().build());
    }

    @Override
    public void refreshCompany() {
        for (String market : getMarkets()) {
            log.info("Requesting company list refresh for market: {}", market);
            publishRequest(FetchCompanyRequest.builder().market(market).build());
        }
    }

    @Override
    public void refreshFundamentals() {
        List<String> templates = Stream.of(StatementTemplate.values()).map(StatementTemplate::getValue).toList();
        for (String market : getMarkets()) {
            for (StatementVariant variant : StatementVariant.values()) {
                log.info("Requesting income statement refresh for market: {}, variant: {}", market, variant);
                publishRequest(new FetchIncomeStatementRequest(market, variant.getValue(), templates));

                log.info("Requesting balance sheet refresh for market: {}, variant: {}", market, variant);
                publishRequest(new FetchBalanceSheetRequest(market, variant.getValue(), templates));

                log.info("Requesting cash flow statement refresh for market: {}, variant: {}", market, variant);
                publishRequest(new FetchCashFlowStatementRequest(market, variant.getValue(), templates));
            }
        }
    }

    @Override
    public void refreshSharePrices(boolean incremental, LocalDate fromDate) {
        var fromTradeDate = fromDate;
        if (incremental && fromDate == null) {
            fromTradeDate = companyApi.getSharePricesLastTradeDate()
                    .map(tradeDate -> tradeDate.minusDays(sharePriceIncrementalWindowDays))
                    .orElse(null);
        }
        String fromTradeDateString = DateTimeUtil.toIsoDate(fromTradeDate);
        for (String market : getMarkets()) {
            if (fromTradeDateString == null) {
                log.info("Requesting full share price refresh for market: {}", market);
            } else {
                log.info("Requesting share price refresh for market: {}, from: {}", market, fromTradeDateString);
            }
            HarvesterRequest request = new FetchSharePricePriceRequest(market, "daily", fromTradeDateString);
            publishRequest(request);
        }
    }

    @Override
    public void refreshNews() {
        OffsetDateTime lastNewsDateTime = companyApi.getNewsLastTimePublished().orElseGet(() -> OffsetDateTime.now().minusDays(1));
        String timeFrom = DateTimeUtil.toIsoCompactDateTime(lastNewsDateTime.minusHours(newsIncrementalWindowHours));

        log.info("Requesting incremental news refresh from: {}", timeFrom);
        HarvesterRequest request = FetchNewsRequest.builder().timeFrom(timeFrom).build();
        publishRequest(request);
    }
}
