package com.oraculum.company.api.dto;

import com.oraculum.company.domain.NewsTickerEntity;

import java.time.OffsetDateTime;

public record NewsTickerDto(String newsId, String ticker, OffsetDateTime timePublished, Float relevanceScore,
                            Float tickerSentimentScore, String tickerSentimentLabel) {
    public static NewsTickerDto fromEntity(NewsTickerEntity entity) {
        if (entity == null) return null;
        return new NewsTickerDto(entity.getNewsId(), entity.getTicker(), entity.getTimePublished(),
                entity.getRelevanceScore(), entity.getTickerSentimentScore(), entity.getTickerSentimentLabel());
    }

    public NewsTickerEntity toEntity() {
        NewsTickerEntity entity = new NewsTickerEntity();
        entity.setNewsId(this.newsId);
        entity.setTicker(this.ticker);
        entity.setTimePublished(this.timePublished);
        entity.setRelevanceScore(this.relevanceScore);
        entity.setTickerSentimentScore(this.tickerSentimentScore);
        entity.setTickerSentimentLabel(this.tickerSentimentLabel);
        return entity;
    }
}