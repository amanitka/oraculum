package com.oraculum.analyst.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;

import java.time.LocalDate;

public record CompanyFinancialRatiosSlim(

        // ── Period ──────────────────────────────────────────────────────────────
        @JsonProperty("citation_id")   String    citationId,
        @JsonProperty("fiscal_year")   int       fiscalYear,
        @JsonProperty("fiscal_period") String    fiscalPeriod,
        @JsonProperty("report_date")   LocalDate reportDate,

        // ── Absolute values ──────────────────────────────────────────────────────
        @JsonProperty("revenue")          Float revenue,
        @JsonProperty("net_income")       Float netIncome,
        @JsonProperty("ebitda")           Float ebitda,
        @JsonProperty("free_cash_flow")   Float freeCashFlow,
        @JsonProperty("earnings_per_share") Float earningsPerShare,

        // ── Profitability ratios ─────────────────────────────────────────────────
        @JsonProperty("return_on_equity")    Float returnOnEquity,
        @JsonProperty("net_margin")          Float netMargin,
        @JsonProperty("gross_margin")        Float grossMargin,
        @JsonProperty("operating_margin")    Float operatingMargin,
        @JsonProperty("fcf_margin")          Float fcfMargin,

        // ── Balance sheet / leverage ratios ─────────────────────────────────────
        @JsonProperty("current_ratio")          Float currentRatio,
        @JsonProperty("quick_ratio")            Float quickRatio,
        @JsonProperty("debt_to_equity")         Float debtToEquity,
        @JsonProperty("interest_coverage_ratio") Float interestCoverageRatio,

        // ── YoY growth rates ─────────────────────────────────────────────────────
        @JsonProperty("revenue_yoy_growth")    Float revenueYoyGrowth,
        @JsonProperty("net_income_yoy_growth") Float netIncomeYoyGrowth,
        @JsonProperty("eps_yoy_growth")        Float epsYoyGrowth,
        @JsonProperty("ebitda_yoy_growth")     Float ebitdaYoyGrowth,
        @JsonProperty("fcf_yoy_growth")        Float fcfYoyGrowth,

        // ── Quality signals ──────────────────────────────────────────────────────
        @JsonProperty("financial_trend_score")       Integer financialTrendScore,
        @JsonProperty("margin_expansion_signal") Integer marginExpansionSignal,
        @JsonProperty("earnings_quality_ratio")  Float   earningsQualityRatio,
        @JsonProperty("is_cash_earnings")        Integer isCashEarnings,
        @JsonProperty("is_negative_equity")      Integer isNegativeEquity,

        // ── Consistency streaks ──────────────────────────────────────────────────
        @JsonProperty("revenue_growth_streak")    Integer revenueGrowthStreak,
        @JsonProperty("positive_fcf_streak")      Integer positiveFcfStreak,
        @JsonProperty("positive_earnings_streak") Integer positiveEarningsStreak

) {
    public static CompanyFinancialRatiosSlim from(CompanyFinancialRatiosDto dto, String citationId) {
        if (dto == null) return null;
        return new CompanyFinancialRatiosSlim(
                citationId,
                dto.fiscalYear(),
                dto.fiscalPeriod(),
                dto.reportDate(),
                dto.revenue(),
                dto.netIncome(),
                dto.ebitda(),
                dto.freeCashFlow(),
                dto.earningsPerShare(),
                dto.returnOnEquity(),
                dto.netMargin(),
                dto.grossMargin(),
                dto.operatingMargin(),
                dto.fcfMargin(),
                dto.currentRatio(),
                dto.quickRatio(),
                dto.debtToEquity(),
                dto.interestCoverageRatio(),
                dto.revenueYoyGrowth(),
                dto.netIncomeYoyGrowth(),
                dto.epsYoyGrowth(),
                dto.ebitdaYoyGrowth(),
                dto.fcfYoyGrowth(),
                dto.financialTrendScore(),
                dto.marginExpansionSignal(),
                dto.earningsQualityRatio(),
                dto.isCashEarnings(),
                dto.isNegativeEquity(),
                dto.revenueGrowthStreak(),
                dto.positiveFcfStreak(),
                dto.positiveEarningsStreak()
        );
    }
}
