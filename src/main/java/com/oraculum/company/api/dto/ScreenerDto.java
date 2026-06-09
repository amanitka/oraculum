package com.oraculum.company.api.dto;

import com.oraculum.company.domain.BaseScreenerEntity;

import java.time.LocalDate;

public record ScreenerDto(
        LocalDate tradeDate,
        int companyId,
        String ticker,
        String market,
        String currency,
        String sectorName,
        String industryName,
        String companySize,
        Float marketCapitalization,
        Float sharePrice,
        Float volumeVelocity,
        Float peRatio,
        Float earningsYield,
        Integer piotroskiFScore,
        Float qualityScore,
        String compositeSignal
) {
    public static ScreenerDto fromEntity(BaseScreenerEntity entity) {
        if (entity == null) return null;
        return new ScreenerDto(
                entity.getTradeDate(),
                entity.getCompanyId(),
                entity.getTicker(),
                entity.getMarket(),
                entity.getCurrency(),
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
                entity.getCompositeSignal()
        );
    }
}
