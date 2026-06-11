package com.oraculum.company.api.dto;

import com.oraculum.company.domain.TickerNewsSentimentEntity;

public record TickerNewsSentimentDto(
        String ticker,
        Integer newsCount7d,
        Float newsSentiment7d,
        Float avgRelevance7d,
        String newsSentimentLabel7d,
        Integer newsCount14d,
        Float newsSentiment14d,
        Float avgRelevance14d,
        String newsSentimentLabel14d,
        Integer newsCount30d,
        Float newsSentiment30d,
        Float avgRelevance30d,
        String newsSentimentLabel30d
) {
    public static TickerNewsSentimentDto fromEntity(TickerNewsSentimentEntity entity) {
        if (entity == null) {
            return null;
        }
        return new TickerNewsSentimentDto(
                entity.getTicker(),
                entity.getNewsCount7d(),
                entity.getNewsSentiment7d(),
                entity.getAvgRelevance7d(),
                entity.getNewsSentimentLabel7d(),
                entity.getNewsCount14d(),
                entity.getNewsSentiment14d(),
                entity.getAvgRelevance14d(),
                entity.getNewsSentimentLabel14d(),
                entity.getNewsCount30d(),
                entity.getNewsSentiment30d(),
                entity.getAvgRelevance30d(),
                entity.getNewsSentimentLabel30d()
        );
    }
}
