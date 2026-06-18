package com.oraculum.company.domain;

import com.oraculum.company.api.domain.StatementTemplate;
import com.oraculum.company.api.domain.StatementVariant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

@Entity
@Immutable
@Table(name = "v_company_financial_ratios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyFinancialRatiosEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "company_id")
    private int companyId;

    @Column(name = "ticker")
    private String ticker;

    @Column(name = "currency")
    private String currency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatementTemplate template;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatementVariant variant;

    @Column(name = "fiscal_year")
    private int fiscalYear;

    @Column(name = "fiscal_period")
    private String fiscalPeriod;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "publish_date")
    private LocalDate publishDate;

    @Column(name = "restated_date")
    private LocalDate restatedDate;

    @Column(name = "ebitda")
    private Float ebitda;

    @Column(name = "free_cash_flow")
    private Float freeCashFlow;

    @Column(name = "ncav")
    private Float ncav;

    @Column(name = "net_net_working_capital")
    private Float netNetWorkingCapital;

    @Column(name = "shares_stabilized")
    private Float sharesStabilized;

    @Column(name = "return_on_capital_employed")
    private Float returnOnCapitalEmployed;

    @Column(name = "return_on_equity")
    private Float returnOnEquity;

    @Column(name = "return_on_assets")
    private Float returnOnAssets;

    @Column(name = "net_margin")
    private Float netMargin;

    @Column(name = "revenue")
    private Float revenue;

    @Column(name = "net_income")
    private Float netIncome;

    @Column(name = "gross_margin")
    private Float grossMargin;

    @Column(name = "operating_margin")
    private Float operatingMargin;

    @Column(name = "fcf_margin")
    private Float fcfMargin;

    @Column(name = "current_ratio")
    private Float currentRatio;

    @Column(name = "quick_ratio")
    private Float quickRatio;

    @Column(name = "debt_to_equity")
    private Float debtToEquity;

    @Column(name = "earnings_per_share")
    private Float earningsPerShare;

    @Column(name = "interest_coverage_ratio")
    private Float interestCoverageRatio;

    @Column(name = "revenue_yoy_growth")
    private Float revenueYoyGrowth;

    @Column(name = "net_income_yoy_growth")
    private Float netIncomeYoyGrowth;

    @Column(name = "ebitda_yoy_growth")
    private Float ebitdaYoyGrowth;

    @Column(name = "fcf_yoy_growth")
    private Float fcfYoyGrowth;

    @Column(name = "eps_yoy_growth")
    private Float epsYoyGrowth;

    @Column(name = "piotroski_f_score")
    private Integer piotroskiFScore;

    @Column(name = "earnings_quality_ratio")
    private Float earningsQualityRatio;

    @Column(name = "is_cash_earnings")
    private Integer isCashEarnings;

    @Column(name = "is_negative_equity")
    private Integer isNegativeEquity;

    @Column(name = "margin_expansion_signal")
    private Integer marginExpansionSignal;

    @Column(name = "revenue_growth_streak")
    private Integer revenueGrowthStreak;

    @Column(name = "positive_fcf_streak")
    private Integer positiveFcfStreak;

    @Column(name = "positive_earnings_streak")
    private Integer positiveEarningsStreak;

    @Column(name = "quality_score")
    private Float qualityScore;
}