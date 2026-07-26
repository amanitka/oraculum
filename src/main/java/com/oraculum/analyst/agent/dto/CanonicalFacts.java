package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import java.time.LocalDate;

/**
 * Pre-computed authoritative metrics sourced from Financial Ratios TTM.
 * All growth/margin values are DECIMALS: 1.2379 = 123.79%, 0.503 = 50.3%.
 * Injected into specialist agent prompts to prevent independent re-derivation.
 */
public record CanonicalFacts(
    @JsonProperty("as_of_date")             LocalDate asOfDate,
    @JsonProperty("eps_yoy_growth_ttm")     Float epsYoyGrowthTtm,
    @JsonProperty("revenue_yoy_growth_ttm") Float revenueYoyGrowthTtm,
    @JsonProperty("net_income_yoy_growth_ttm") Float netIncomeYoyGrowthTtm,
    @JsonProperty("fcf_yoy_growth_ttm")     Float fcfYoyGrowthTtm,
    @JsonProperty("net_margin_ttm")         Float netMarginTtm,
    @JsonProperty("gross_margin_ttm")       Float grossMarginTtm,
    @JsonProperty("operating_margin_ttm")   Float operatingMarginTtm,
    @JsonProperty("fcf_margin_ttm")         Float fcfMarginTtm,
    @JsonProperty("roe_ttm")                Float roeTtm,
    @JsonProperty("roce_ttm")               Float roceTtm,
    @JsonProperty("debt_to_equity_ttm")     Float debtToEquityTtm,
    @JsonProperty("current_ratio_ttm")      Float currentRatioTtm,
    @JsonProperty("eps_ttm")                Float epsTtm,
    @JsonProperty("free_cash_flow_ttm")     Float freeCashFlowTtm,
    @JsonProperty("revenue_ttm")            Float revenueTtm,
    @JsonProperty("net_income_ttm")         Float netIncomeTtm,
    // Latest annual — for time-window labelled comparison
    @JsonProperty("net_margin_a")          Float netMarginA,
    @JsonProperty("gross_margin_a")        Float grossMarginA,
    @JsonProperty("roe_a")                 Float roeA,
    @JsonProperty("report_date_a")         LocalDate reportDateA
) {
    public static CanonicalFacts from(CompanyFinancialRatiosDto ttm, CompanyFinancialRatiosDto annual) {
        return new CanonicalFacts(
                ttm    != null ? ttm.reportDate()              : null,
                ttm    != null ? ttm.epsYoyGrowth()            : null,
                ttm    != null ? ttm.revenueYoyGrowth()        : null,
                ttm    != null ? ttm.netIncomeYoyGrowth()      : null,
                ttm    != null ? ttm.fcfYoyGrowth()            : null,
                ttm    != null ? ttm.netMargin()               : null,
                ttm    != null ? ttm.grossMargin()             : null,
                ttm    != null ? ttm.operatingMargin()         : null,
                ttm    != null ? ttm.fcfMargin()               : null,
                ttm    != null ? ttm.returnOnEquity()          : null,
                ttm    != null ? ttm.returnOnCapitalEmployed() : null,
                ttm    != null ? ttm.debtToEquity()            : null,
                ttm    != null ? ttm.currentRatio()            : null,
                ttm    != null ? ttm.earningsPerShare()        : null,
                ttm    != null ? ttm.freeCashFlow()            : null,
                ttm    != null ? ttm.revenue()                 : null,
                ttm    != null ? ttm.netIncome()               : null,
                annual != null ? annual.netMargin()            : null,
                annual != null ? annual.grossMargin()          : null,
                annual != null ? annual.returnOnEquity()       : null,
                annual != null ? annual.reportDate()           : null
        );
    }
}
