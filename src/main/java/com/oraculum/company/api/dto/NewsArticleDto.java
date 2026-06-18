package com.oraculum.company.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.common.config.LenientLocalDateTimeDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record NewsArticleDto(String id,
                             String title,
                             String url,
                             @JsonProperty("time_published") @JsonDeserialize(using = LenientLocalDateTimeDeserializer.class) LocalDateTime timePublished,
                             List<String> authors,
                             String summary,
                             @JsonProperty("banner_image") String bannerImage,
                             String source,
                             @JsonProperty("category_within_source") String categoryWithinSource,
                             @JsonProperty("source_domain") String sourceDomain,
                             List<TopicRelevanceDto> topics,
                             @JsonProperty("overall_sentiment_score") Float overallSentimentScore,
                             @JsonProperty("overall_sentiment_label") String overallSentimentLabel,
                             @JsonProperty("extracted_at") LocalDate extractedAt,
                             @JsonProperty("sentiment_score_definition") String sentimentScoreDefinition,
                             @JsonProperty("relevance_score_definition") String relevanceScoreDefinition,
                             @JsonProperty("ticker_sentiment") List<NewsTickerSentimentDto> tickerSentiment) {

    public record NewsTickerSentimentDto(String ticker,
                                         @JsonProperty("relevance_score") Float relevanceScore,
                                         @JsonProperty("ticker_sentiment_score") Float tickerSentimentScore,
                                         @JsonProperty("ticker_sentiment_label") String tickerSentimentLabel) {
    }

    public record TopicRelevanceDto(String topic,
                                    @JsonProperty("relevance_score") Float relevanceScore) {
    }
}
