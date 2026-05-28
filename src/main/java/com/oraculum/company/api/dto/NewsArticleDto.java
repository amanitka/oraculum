package com.oraculum.company.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.common.jackson.LenientLocalDateTimeDeserializer;
import com.oraculum.company.domain.NewsEntity;
import com.oraculum.company.domain.NewsTickerEntity;
import com.oraculum.util.TimeUtil;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record NewsArticleDto(String id, String title, String url,
                             @JsonProperty("time_published") @JsonDeserialize(using =
                                     LenientLocalDateTimeDeserializer.class) LocalDateTime timePublished,
                             List<String> authors, String summary, @JsonProperty("banner_image") String bannerImage,
                             String source, @JsonProperty("category_within_source") String categoryWithinSource,
                             @JsonProperty("source_domain") String sourceDomain, List<TopicRelevanceDto> topics,
                             @JsonProperty("overall_sentiment_score") Float overallSentimentScore,
                             @JsonProperty("overall_sentiment_label") String overallSentimentLabel,
                             @JsonProperty("extracted_at") LocalDate extractedAt,
                             @JsonProperty("sentiment_score_definition") String sentimentScoreDefinition,
                             @JsonProperty("relevance_score_definition") String relevanceScoreDefinition,
                             @JsonProperty("ticker_sentiment") List<NewsTickerSentimentDto> tickerSentiment) {
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private static String toJsonString(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize news payload to JSON", e);
        }
    }

    public NewsEntity toNewsEntity() {
        NewsEntity entity = new NewsEntity();
        entity.setId(this.id);
        entity.setTitle(this.title);
        entity.setUrl(this.url);
        entity.setTimePublished(TimeUtil.toOffsetDateTime(this.timePublished));
        entity.setAuthors(toJsonString(this.authors));
        entity.setSummary(this.summary);
        entity.setSource(this.source);
        entity.setCategoryWithinSource(this.categoryWithinSource);
        entity.setSourceDomain(this.sourceDomain);
        entity.setTopics(toJsonString(this.topics));
        entity.setOverallSentimentScore(this.overallSentimentScore);
        entity.setOverallSentimentLabel(this.overallSentimentLabel);
        entity.setExtractedAt(TimeUtil.toOffsetDateTime(this.extractedAt));
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
            entity.setTimePublished(TimeUtil.toOffsetDateTime(this.timePublished));
            entity.setRelevanceScore(s.relevanceScore());
            entity.setTickerSentimentScore(s.tickerSentimentScore());
            entity.setTickerSentimentLabel(s.tickerSentimentLabel());
            return entity;
        }).collect(Collectors.toList());
    }

    public record NewsTickerSentimentDto(String ticker, @JsonProperty("relevance_score") Float relevanceScore,
                                         @JsonProperty("ticker_sentiment_score") Float tickerSentimentScore,
                                         @JsonProperty("ticker_sentiment_label") String tickerSentimentLabel) {
    }

    public record TopicRelevanceDto(String topic, @JsonProperty("relevance_score") Float relevanceScore) {
    }
}
