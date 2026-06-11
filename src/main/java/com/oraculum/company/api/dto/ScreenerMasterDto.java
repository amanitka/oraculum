package com.oraculum.company.api.dto;

import com.oraculum.company.api.domain.CompanySize;
import com.oraculum.company.domain.ScreenerMasterEntity;

import java.time.LocalDate;

public record ScreenerMasterDto(
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
        Long qualityRank,
        Long valueRank,
        Long fscoreRank,
        Float newsSentimentScore,
        String newsSentimentLabel,
        Integer newsCount30d
) {
    public static ScreenerMasterDto fromEntity(ScreenerMasterEntity entity) {
        if (entity == null) return null;
        return new ScreenerMasterDto(
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
                entity.getQualityRank(),
                entity.getValueRank(),
                entity.getFscoreRank(),
                entity.getNewsSentimentScore(),
                entity.getNewsSentimentLabel(),
                entity.getNewsCount30d()
        );
    }
}
