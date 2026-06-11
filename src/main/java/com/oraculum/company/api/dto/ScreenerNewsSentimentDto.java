package com.oraculum.company.api.dto;

import com.oraculum.company.api.domain.CompanySize;
import com.oraculum.company.domain.ScreenerNewsSentimentEntity;

import java.time.LocalDate;

public record ScreenerNewsSentimentDto(
        LocalDate tradeDate,
        int companyId,
        String ticker,
        String market,
        String currency,
        String companyName,
        String description,
        String sectorName,
        String industryName,
        CompanySize companySize,
        Float marketCapitalization,
        Float sharePrice,
        Float volumeVelocity,
        Float peRatio,
        Float earningsYield,
        Integer piotroskiFScore,
        Float qualityScore,
        String compositeSignal,
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
    public static ScreenerNewsSentimentDto fromEntity(ScreenerNewsSentimentEntity entity) {
        if (entity == null) return null;
        return new ScreenerNewsSentimentDto(
                entity.getTradeDate(),
                entity.getCompanyId(),
                entity.getTicker(),
                entity.getMarket(),
                entity.getCurrency(),
                entity.getCompanyName(),
                entity.getDescription(),
                entity.getSectorName(),
                entity.getIndustryName(),
                entity.getCompanySize(),
                entity.getMarketCapitalization(),
                entity.getSharePrice(),
                entity.getVolumeVelocity(),
                entity.getPeRatio(),
                entity.getEarningsYield(),
                entity.getPiotroskiFScore(),
                entity.getQualityScore(),
                entity.getCompositeSignal(),
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
