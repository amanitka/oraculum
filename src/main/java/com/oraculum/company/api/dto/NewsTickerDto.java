package com.oraculum.company.api.dto;

import com.oraculum.company.domain.NewsEntity;
import com.oraculum.company.domain.NewsTickerEntity;

import java.time.OffsetDateTime;

public record NewsTickerDto(
        String id,
        String title,
        String url,
        OffsetDateTime timePublished,
        String authors,
        String summary,
        String source,
        String categoryWithinSource,
        String sourceDomain,
        String topics,
        Float overallSentimentScore,
        String overallSentimentLabel,
        OffsetDateTime extractedAt,
        String sentimentScoreDefinition,
        String relevanceScoreDefinition,
        String ticker,
        Float relevanceScore,
        Float tickerSentimentScore,
        String tickerSentimentLabel
) {
    public static NewsTickerDto from(NewsEntity news, NewsTickerEntity tickerSentiment) {
        return new NewsTickerDto(
                news.getId(),
                news.getTitle(),
                news.getUrl(),
                news.getTimePublished(),
                news.getAuthors(),
                news.getSummary(),
                news.getSource(),
                news.getCategoryWithinSource(),
                news.getSourceDomain(),
                news.getTopics(),
                news.getOverallSentimentScore(),
                news.getOverallSentimentLabel(),
                news.getExtractedAt(),
                news.getSentimentScoreDefinition(),
                news.getRelevanceScoreDefinition(),
                tickerSentiment.getTicker(),
                tickerSentiment.getRelevanceScore(),
                tickerSentiment.getTickerSentimentScore(),
                tickerSentiment.getTickerSentimentLabel()
        );
    }
}