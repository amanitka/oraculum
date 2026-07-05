package com.oraculum.company.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oraculum.company.domain.NewsEntity;
import com.oraculum.company.domain.NewsTickerEntity;
import com.oraculum.company.api.dto.NewsArticleDto.TopicRelevanceDto;

import java.time.OffsetDateTime;
import java.util.List;

public record NewsTickerDto(
        @JsonIgnore String id,
        String title,
        @JsonIgnore String url,
        OffsetDateTime timePublished,
        @JsonIgnore List<String> authors,
        String summary,
        String source,
        @JsonIgnore String categoryWithinSource,
        @JsonIgnore String sourceDomain,
        @JsonIgnore List<TopicRelevanceDto> topics,
        Float overallSentimentScore,
        String overallSentimentLabel,
        @JsonIgnore OffsetDateTime extractedAt,
        @JsonIgnore String sentimentScoreDefinition,
        @JsonIgnore String relevanceScoreDefinition,
        @JsonIgnore String ticker,
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
