package com.oraculum.company.api.dto;

import com.oraculum.company.domain.NewsEntity;
import com.oraculum.company.domain.NewsTickerEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record NewsArticleDto(
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
        List<TickerSentimentDto> tickerSentiment
) {
    public record TickerSentimentDto(
            String ticker,
            Float relevanceScore,
            Float tickerSentimentScore,
            String tickerSentimentLabel
    ) {}

    public NewsEntity toNewsEntity() {
        NewsEntity entity = new NewsEntity();
        entity.setId(this.id);
        entity.setTitle(this.title);
        entity.setUrl(this.url);
        entity.setTimePublished(this.timePublished);
        entity.setAuthors(this.authors);
        entity.setSummary(this.summary);
        entity.setSource(this.source);
        entity.setCategoryWithinSource(this.categoryWithinSource);
        entity.setSourceDomain(this.sourceDomain);
        entity.setTopics(this.topics);
        entity.setOverallSentimentScore(this.overallSentimentScore);
        entity.setOverallSentimentLabel(this.overallSentimentLabel);
        entity.setExtractedAt(this.extractedAt);
        entity.setSentimentScoreDefinition(this.sentimentScoreDefinition);
        entity.setRelevanceScoreDefinition(this.relevanceScoreDefinition);
        return entity;
    }

    public List<NewsTickerEntity> toNewsTickerEntities() {
        if (tickerSentiment == null) {
            return List.of();
        }
        return tickerSentiment.stream().map(s -> {
            NewsTickerEntity entity = new NewsTickerEntity();
            entity.setNewsId(this.id);
            entity.setTicker(s.ticker());
            entity.setTimePublished(this.timePublished);
            entity.setRelevanceScore(s.relevanceScore());
            entity.setTickerSentimentScore(s.tickerSentimentScore());
            entity.setTickerSentimentLabel(s.tickerSentimentLabel());
            return entity;
        }).collect(Collectors.toList());
    }
}
