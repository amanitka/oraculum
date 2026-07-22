package com.oraculum.company.api.dto;

import com.oraculum.company.api.domain.CompanySize;
import com.oraculum.company.domain.CompanyOverviewEntity;

import java.time.LocalDate;

public record CompanyOverviewDto(
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
        Float priceChange1d,
        Float priceChange1w,
        Float priceChange1m,
        Float peRatio,
        Float earningsYield,
        Integer financialTrendScore,
        Float qualityScore,
        String compositeSignal,
        Long qualityRank,
        Long valueRank,
        Long fscoreRank,
        Float newsSentimentScore,
        String newsSentimentLabel,
        Integer newsCount30d
) {
    public static CompanyOverviewDto fromEntity(CompanyOverviewEntity entity) {
        if (entity == null) return null;
        return new CompanyOverviewDto(
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
                entity.getPriceChange1d(),
                entity.getPriceChange1w(),
                entity.getPriceChange1m(),
                entity.getPeRatio(),
                entity.getEarningsYield(),
                entity.getFinancialTrendScore(),
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
