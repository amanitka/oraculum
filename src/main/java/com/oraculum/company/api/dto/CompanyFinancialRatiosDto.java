package com.oraculum.company.api.dto;

import com.oraculum.company.api.domain.StatementTemplate;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.domain.CompanyFinancialRatiosEntity;

import java.time.LocalDate;
import java.util.Comparator;

public record CompanyFinancialRatiosDto(String id,
                                        int companyId,
                                        String ticker,
                                        String currency,
                                        StatementTemplate template,
                                        StatementVariant variant,
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
                                        Float returnOnCapitalEmployed,
                                        Float returnOnEquity,
                                        Float returnOnAssets,
                                        Float netMargin,
                                        Float revenue,
                                        Float netIncome,
                                        Float grossMargin,
                                        Float operatingMargin,
                                        Float fcfMargin,
                                        Float currentRatio,
                                        Float quickRatio,
                                        Float debtToEquity,
                                        Float earningsPerShare,
                                        Float interestCoverageRatio,
                                        Float revenueYoyGrowth,
                                        Float netIncomeYoyGrowth,
                                        Float ebitdaYoyGrowth,
                                        Float fcfYoyGrowth,
                                        Float epsYoyGrowth,
                                        Integer financialTrendScore,
                                        Float earningsQualityRatio,
                                        Integer isCashEarnings,
                                        Integer isNegativeEquity,
                                        Integer marginExpansionSignal,
                                        Integer revenueGrowthStreak,
                                        Integer positiveFcfStreak,
                                        Integer positiveEarningsStreak,
                                        Float qualityScore) {

    public static CompanyFinancialRatiosDto fromEntity(CompanyFinancialRatiosEntity entity) {
        if (entity == null)
            return null;
        return new CompanyFinancialRatiosDto(entity.getId(),
                entity.getCompanyId(),
                entity.getTicker(),
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
                entity.getReturnOnCapitalEmployed(),
                entity.getReturnOnEquity(),
                entity.getReturnOnAssets(),
                entity.getNetMargin(),
                entity.getRevenue(),
                entity.getNetIncome(),
                entity.getGrossMargin(),
                entity.getOperatingMargin(),
                entity.getFcfMargin(),
                entity.getCurrentRatio(),
                entity.getQuickRatio(),
                entity.getDebtToEquity(),
                entity.getEarningsPerShare(),
                entity.getInterestCoverageRatio(),
                entity.getRevenueYoyGrowth(),
                entity.getNetIncomeYoyGrowth(),
                entity.getEbitdaYoyGrowth(),
                entity.getFcfYoyGrowth(),
                entity.getEpsYoyGrowth(),
                entity.getFinancialTrendScore(),
                entity.getEarningsQualityRatio(),
                entity.getIsCashEarnings(),
                entity.getIsNegativeEquity(),
                entity.getMarginExpansionSignal(),
                entity.getRevenueGrowthStreak(),
                entity.getPositiveFcfStreak(),
                entity.getPositiveEarningsStreak(),
                entity.getQualityScore());
    }

    public static Comparator<CompanyFinancialRatiosDto> getComparator() {
        return Comparator.comparing(CompanyFinancialRatiosDto::fiscalYear)
                .thenComparing(CompanyFinancialRatiosDto::fiscalPeriod);
    }
}
