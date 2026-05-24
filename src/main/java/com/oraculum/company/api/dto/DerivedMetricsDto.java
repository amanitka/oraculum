package com.oraculum.company.api.dto;

import com.oraculum.company.domain.DerivedMetricsEntity;

import java.time.LocalDate;

public record DerivedMetricsDto(
    String compositeKey,
    String ticker,
    int simfinId,
    String currency,
    String template,
    String variant,
    int fiscalYear,
    String fiscalPeriod,
    LocalDate reportDate,
    LocalDate publishDate,
    LocalDate restatedDate,
    Float ebitda,
    Float freeCashFlow,
    Float ncav,
    Float netNetWorkingCapital,
    Float sharesStabilized,
    Float returnOnEquity,
    Float netMargin,
    Float revenue,
    Float netIncome
) {
    public static DerivedMetricsDto fromEntity(DerivedMetricsEntity entity) {
        if (entity == null) return null;
        return new DerivedMetricsDto(
            entity.getCompositeKey(),
            entity.getTicker(),
            entity.getSimfinId(),
            entity.getCurrency(),
            entity.getTemplate(),
            entity.getVariant(),
            entity.getFiscalYear(),
            entity.getFiscalPeriod(),
            entity.getReportDate(),
            entity.getPublishDate(),
            entity.getRestatedDate(),
            entity.getEbitda(),
            entity.getFreeCashFlow(),
            entity.getNcav(),
            entity.getNetNetWorkingCapital(),
            entity.getSharesStabilized(),
            entity.getReturnOnEquity(),
            entity.getNetMargin(),
            entity.getRevenue(),
            entity.getNetIncome()
        );
    }
}