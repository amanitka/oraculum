package com.oraculum.company.api.dto;

import com.oraculum.company.api.domain.CompanySize;
import com.oraculum.company.domain.CompanyOverviewBaseEntity;

import java.time.LocalDate;

public record ScreenerDto(
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
        Integer financialTrendScore,
        Float qualityScore,
        String compositeSignal
) {
    public static ScreenerDto fromEntity(CompanyOverviewBaseEntity entity) {
        if (entity == null) return null;
        return new ScreenerDto(
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
                entity.getFinancialTrendScore(),
                entity.getQualityScore(),
                entity.getCompositeSignal()
        );
    }
}
