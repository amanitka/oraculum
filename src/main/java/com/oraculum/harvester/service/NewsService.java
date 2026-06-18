package com.oraculum.harvester.service;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.company.api.CompanyApi;
import com.oraculum.company.api.CompanyLoadApi;
import com.oraculum.company.api.dto.NewsArticleDto;
import com.oraculum.harvester.domain.ProviderType;
import com.oraculum.harvester.provider.AlphaVantageClient;
import com.oraculum.harvester.provider.dto.AlphaVantageNewsResponse;
import com.oraculum.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Slf4j
public class NewsService {

    private final AlphaVantageClient alphaVantageClient;
    private final CompanyLoadApi companyLoadApi;
    private final CompanyApi companyApi;
    private final ApiUsageTrackerService apiUsageTrackerService;
    private final int newsIncrementalWindowHours;
    private final int dailyLimit;

    public NewsService(AlphaVantageClient alphaVantageClient,
                       CompanyLoadApi companyLoadApi,
                       CompanyApi companyApi,
                       ApiUsageTrackerService apiUsageTrackerService,
                       OraculumProperties properties) {
        this.alphaVantageClient = alphaVantageClient;
        this.companyLoadApi = companyLoadApi;
        this.companyApi = companyApi;
        this.apiUsageTrackerService = apiUsageTrackerService;
        this.newsIncrementalWindowHours = properties.data().news().incrementalWindowHours();
        this.dailyLimit = properties.harvester().alphaVantage().dailyLimit();
    }

    private String getNewsFromDateTime() {
        OffsetDateTime lastNewsDateTime = companyApi.getNewsLastTimePublished().orElseGet(() -> OffsetDateTime.now().minusDays(1));
        return DateTimeUtil.toIsoCompactDateTime(lastNewsDateTime.minusHours(newsIncrementalWindowHours));
    }

    private void createOrUpdateNews(AlphaVantageNewsResponse response) {
        List<NewsArticleDto> enrichedArticles = response.feed().stream()
                .map(article -> enrichArticle(article, response))
                .toList();
        companyLoadApi.createOrUpdateNewsBatch(enrichedArticles);
        log.info("Successfully loaded {} news into database.", enrichedArticles.size());
    }

    public void refreshNews() {
        if (!apiUsageTrackerService.canMakeCall(ProviderType.ALPHA_VANTAGE, dailyLimit)) {
            log.warn("Alpha Vantage daily limit reached. Skipping news refresh.");
            return;
        }
        String timeFrom = getNewsFromDateTime();
        log.info("Fetching incremental news from Alpha Vantage from: {}", timeFrom);
        try {
            var response = alphaVantageClient.fetchNewsSentiment(timeFrom);
            apiUsageTrackerService.recordCall(ProviderType.ALPHA_VANTAGE);
            if (response == null || response.feed() == null || response.feed().isEmpty()) {
                log.info("No new articles found from Alpha Vantage.");
                return;
            }
            createOrUpdateNews(response);
        } catch (Exception e) {
            log.error("Failed to fetch or process news from Alpha Vantage", e);
        }
    }

    private NewsArticleDto enrichArticle(NewsArticleDto original, AlphaVantageNewsResponse response) {
        String id = generateArticleId(original);
        return new NewsArticleDto(
                id,
                original.title(),
                original.url(),
                original.timePublished(),
                original.authors(),
                original.summary(),
                original.source(),
                original.categoryWithinSource(),
                original.sourceDomain(),
                original.topics(),
                original.overallSentimentScore(),
                original.overallSentimentLabel(),
                OffsetDateTime.now(),
                response.sentimentScoreDefinition(),
                response.relevanceScoreDefinition(),
                original.tickerSentiment()
        );
    }

    private String generateArticleId(NewsArticleDto article) {
        try {
            String url = article.url() != null ? article.url() : "";
            String timePub = article.timePublished() != null ? DateTimeUtil.toIsoCompactDateTime(DateTimeUtil.toOffsetDateTime(article.timePublished())) : "";
            String title = article.title() != null ? article.title() : "";

            String keyStr = String.join("|", url, timePub, title);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(keyStr.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }
}
