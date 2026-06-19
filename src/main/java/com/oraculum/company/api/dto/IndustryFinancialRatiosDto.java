package com.oraculum.company.api.dto;

import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.domain.IndustryFinancialRatiosEntity;

public record IndustryFinancialRatiosDto(
        String industryName,
        StatementVariant variant,
        Integer companyCount,
        Float returnOnEquity,
        Float grossMargin,
        Float netMargin,
        Float debtToEquity,
        Float currentRatio,
        Float operatingMargin,
        Float fcfMargin,
        Float revenueYoyGrowth
) {
    public static IndustryFinancialRatiosDto fromEntity(IndustryFinancialRatiosEntity entity) {
        if (entity == null) return null;
        return new IndustryFinancialRatiosDto(
                entity.getIndustryName(),
                entity.getVariant(),
                entity.getCompanyCount(),
                entity.getReturnOnEquity(),
                entity.getGrossMargin(),
                entity.getNetMargin(),
                entity.getDebtToEquity(),
                entity.getCurrentRatio(),
                entity.getOperatingMargin(),
                entity.getFcfMargin(),
                entity.getRevenueYoyGrowth()
        );
    }
}
