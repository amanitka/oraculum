package com.oraculum.company.api.dto;

import com.oraculum.company.domain.DailyMarketSignalEntity;

import java.time.LocalDate;

public record DailyMarketSignalDto(
    LocalDate tradeDate,
    int companyId,
    String market,
    String flagLastDayOfMonth,
    String currency,
    Float sharePrice,
    Integer volume,
    Float pctFrom50dMa,
    Float pctFrom200dMa,
    Float volumeVelocity,
    Integer activeFiscalYear,
    String activeFiscalPeriod,
    LocalDate activeReportPublishDate,
    Float marketCapitalization,
    Float peRatio,
    Float earningsYield,
    Float priceToFcf,
    Float priceToSales,
    Float priceToBook,
    Float priceToNcav,
    Float priceToNnwc,
    int isGrahamNetNet,
    Float enterpriseValue,
    Float enterpriseValueToEbitda,
    Float returnOnCapitalEmployed,
    Float returnOnEquity,
    Float netMargin,
    Float currentRatio,
    Float debtToEquity
) {
    public static DailyMarketSignalDto fromEntity(DailyMarketSignalEntity entity) {
        if (entity == null) return null;
        return new DailyMarketSignalDto(
            entity.getTradeDate(),
            entity.getCompanyId(),
            entity.getMarket(),
            entity.getFlagLastDayOfMonth(),
            entity.getCurrency(),
            entity.getSharePrice(),
            entity.getVolume(),
            entity.getPctFrom50dMa(),
            entity.getPctFrom200dMa(),
            entity.getVolumeVelocity(),
            entity.getActiveFiscalYear(),
            entity.getActiveFiscalPeriod(),
            entity.getActiveReportPublishDate(),
            entity.getMarketCapitalization(),
            entity.getPeRatio(),
            entity.getEarningsYield(),
            entity.getPriceToFcf(),
            entity.getPriceToSales(),
            entity.getPriceToBook(),
            entity.getPriceToNcav(),
            entity.getPriceToNnwc(),
            entity.getIsGrahamNetNet(),
            entity.getEnterpriseValue(),
            entity.getEnterpriseValueToEbitda(),
            entity.getReturnOnCapitalEmployed(),
            entity.getReturnOnEquity(),
            entity.getNetMargin(),
            entity.getCurrentRatio(),
            entity.getDebtToEquity()
        );
    }
}