package com.oraculum.company.api.dto;

import com.oraculum.company.domain.ScreenerMasterEntity;

import java.time.LocalDate;

public record ScreenerMasterDto(
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
        String compositeSignal,
        Long qualityRank,
        Long valueRank,
        Long fscoreRank
) {
    public static ScreenerMasterDto fromEntity(ScreenerMasterEntity entity) {
        if (entity == null) return null;
        return new ScreenerMasterDto(
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
                entity.getCompositeSignal(),
                entity.getQualityRank(),
                entity.getValueRank(),
                entity.getFscoreRank()
        );
    }
}
