package com.oraculum.harvester.service;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.harvester.api.dto.CompanyNewsDto;
import com.oraculum.harvester.provider.FinnhubClient;
import com.oraculum.harvester.provider.dto.FinnhubCompanyNewsItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class FinnhubNewsService {

    private static final Pattern BOT_ARTICLE_PATTERN = Pattern.compile(
            "\\b(buys|sells|purchases|acquires|holdings|stake|shares of)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final int MAX_SUMMARY_LENGTH = 400;

    private final FinnhubClient finnhubClient;
    private final boolean finnhubConfigured;

    public FinnhubNewsService(FinnhubClient finnhubClient, OraculumProperties properties) {
        this.finnhubClient = finnhubClient;
        this.finnhubConfigured = properties.harvester().finnhub() != null
                && properties.harvester().finnhub().hasApiKey();
    }

    @Cacheable(value = "finnhubCompanyNews", key = "#ticker + '_' + #from + '_' + #to")
    public List<CompanyNewsDto> fetchCompanyNews(String ticker, LocalDate from, LocalDate to) {
        if (!finnhubConfigured || !StringUtils.hasText(ticker) || from == null || to == null) {
            return List.of();
        }

        try {
            List<FinnhubCompanyNewsItem> rawItems = finnhubClient.fetchCompanyNews(ticker.trim(), from, to);
            return processNewsItems(rawItems);
        } catch (Exception e) {
            log.error("Failed to fetch company news from Finnhub for {}", ticker, e);
            return List.of();
        }
    }

    private List<CompanyNewsDto> processNewsItems(List<FinnhubCompanyNewsItem> rawItems) {
        if (rawItems == null || rawItems.isEmpty()) {
            return List.of();
        }
        Set<String> seenHeadlines = new HashSet<>();

        return rawItems.stream()
                .filter(this::isValidArticle)
                .filter(item -> seenHeadlines.add(item.headline().trim().toLowerCase(Locale.ROOT)))
                .map(this::toCompanyNewsDto)
                .sorted(Comparator.comparing(CompanyNewsDto::publishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private boolean isValidArticle(FinnhubCompanyNewsItem item) {
        if (item == null || !StringUtils.hasText(item.headline())) {
            return false;
        }
        return !BOT_ARTICLE_PATTERN.matcher(item.headline()).find();
    }

    private CompanyNewsDto toCompanyNewsDto(FinnhubCompanyNewsItem item) {
        OffsetDateTime publishedAt = item.datetime() != null
                ? Instant.ofEpochSecond(item.datetime()).atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now();
        List<String> related = item.related() != null
                ? Arrays.stream(item.related().split(",")).map(String::trim).filter(StringUtils::hasText).toList()
                : List.of();
        long id = item.id() != null ? item.id() : 0L;

        return new CompanyNewsDto(
                id,
                publishedAt,
                item.headline() != null ? item.headline().trim() : "",
                truncateSummary(item.summary()),
                item.source(),
                item.url(),
                related
        );
    }

    private String truncateSummary(String summary) {
        if (summary == null || summary.length() <= MAX_SUMMARY_LENGTH) {
            return summary;
        }
        int lastSpace = summary.lastIndexOf(' ', MAX_SUMMARY_LENGTH);
        return (lastSpace > 0 ? summary.substring(0, lastSpace) : summary.substring(0, MAX_SUMMARY_LENGTH)) + "...";
    }
}
