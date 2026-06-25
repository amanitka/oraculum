package com.oraculum.analyst.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.company.api.dto.NewsTickerDto;

import java.time.OffsetDateTime;

public record NewsTickerSlim(
        @JsonProperty("citation_id") String citationId,
        String title,
        String url,
        @JsonProperty("time_published") OffsetDateTime timePublished,
        String summary,
        String source,
        @JsonProperty("overall_sentiment_score") Float overallSentimentScore,
        @JsonProperty("overall_sentiment_label") String overallSentimentLabel,
        String ticker,
        @JsonProperty("relevance_score") Float relevanceScore,
        @JsonProperty("ticker_sentiment_score") Float tickerSentimentScore,
        @JsonProperty("ticker_sentiment_label") String tickerSentimentLabel
) {
    public static NewsTickerSlim from(NewsTickerDto dto, String citationId) {
        if (dto == null) return null;
        return new NewsTickerSlim(
                citationId,
                dto.title(),
                dto.url(),
                dto.timePublished(),
                dto.summary(),
                dto.source(),
                dto.overallSentimentScore(),
                dto.overallSentimentLabel(),
                dto.ticker(),
                dto.relevanceScore(),
                dto.tickerSentimentScore(),
                dto.tickerSentimentLabel()
        );
    }
}
