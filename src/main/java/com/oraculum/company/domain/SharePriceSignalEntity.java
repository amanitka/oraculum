package com.oraculum.company.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Immutable
@Table(name = "v_share_price_signals")
@IdClass(SharePriceSignalEntity.SharePriceSignalId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SharePriceSignalEntity {

    @Id
    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @Id
    @Column(name = "company_id")
    private int companyId;

    @Column(name = "ticker")
    private String ticker;

    @Column(name = "market")
    private String market;

    @Column(name = "flag_last_day_of_month")
    private String flagLastDayOfMonth;

    @Column(name = "currency")
    private String currency;

    @Column(name = "share_price")
    private Float sharePrice;

    @Column(name = "volume")
    private Integer volume;

    @Column(name = "pct_from_50d_ma")
    private Float pctFrom50dMa;

    @Column(name = "pct_from_200d_ma")
    private Float pctFrom200dMa;

    @Column(name = "volume_velocity")
    private Float volumeVelocity;

    @Column(name = "active_fiscal_year")
    private Integer activeFiscalYear;

    @Column(name = "active_fiscal_period")
    private String activeFiscalPeriod;

    @Column(name = "active_report_publish_date")
    private LocalDate activeReportPublishDate;

    @Column(name = "market_capitalization")
    private Float marketCapitalization;

    @Column(name = "pe_ratio")
    private Float peRatio;

    @Column(name = "earnings_yield")
    private Float earningsYield;

    @Column(name = "price_to_fcf")
    private Float priceToFcf;

    @Column(name = "price_to_sales")
    private Float priceToSales;

    @Column(name = "price_to_book")
    private Float priceToBook;

    @Column(name = "price_to_ncav")
    private Float priceToNcav;

    @Column(name = "price_to_nnwc")
    private Float priceToNnwc;

    @Column(name = "is_graham_net_net")
    private int isGrahamNetNet;

    @Column(name = "enterprise_value")
    private Float enterpriseValue;

    @Column(name = "enterprise_value_to_ebitda")
    private Float enterpriseValueToEbitda;

    @Column(name = "return_on_capital_employed")
    private Float returnOnCapitalEmployed;

    @Column(name = "return_on_equity")
    private Float returnOnEquity;

    @Column(name = "net_margin")
    private Float netMargin;

    @Column(name = "current_ratio")
    private Float currentRatio;

    @Column(name = "debt_to_equity")
    private Float debtToEquity;

    @Column(name = "fcf_yield")
    private Float fcfYield;

    @Column(name = "enterprise_value_to_revenue")
    private Float enterpriseValueToRevenue;

    @Column(name = "enterprise_value_to_free_cash_flow")
    private Float enterpriseValueToFreeCashFlow;

    @Column(name = "return_on_assets")
    private Float returnOnAssets;

    @Column(name = "gross_margin")
    private Float grossMargin;

    @Column(name = "operating_margin")
    private Float operatingMargin;

    @Column(name = "fcf_margin")
    private Float fcfMargin;

    @Column(name = "quick_ratio")
    private Float quickRatio;

    @Column(name = "interest_coverage_ratio")
    private Float interestCoverageRatio;

    @Column(name = "graham_margin_of_safety")
    private Float grahamMarginOfSafety;

    @Column(name = "is_graham_defensive")
    private Integer isGrahamDefensive;

    @Column(name = "revenue_yoy_growth")
    private Float revenueYoyGrowth;

    @Column(name = "eps_yoy_growth")
    private Float epsYoyGrowth;

    @Column(name = "piotroski_f_score")
    private Integer piotroskiFScore;

    @Column(name = "revenue_growth_streak")
    private Integer revenueGrowthStreak;

    @Column(name = "positive_fcf_streak")
    private Integer positiveFcfStreak;

    @Column(name = "positive_earnings_streak")
    private Integer positiveEarningsStreak;

    @Column(name = "is_cash_earnings")
    private Integer isCashEarnings;

    @Column(name = "is_negative_equity")
    private Integer isNegativeEquity;

    @Column(name = "quality_score")
    private Float qualityScore;

    @Column(name = "composite_signal")
    private String compositeSignal;

    @Column(name = "company_size")
    private String companySize;

    @Column(name = "sector_name")
    private String sectorName;

    @Column(name = "industry_name")
    private String industryName;

    public static class SharePriceSignalId implements Serializable {
        private LocalDate tradeDate;
        private int companyId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SharePriceSignalId that = (SharePriceSignalId) o;
            return companyId == that.companyId && Objects.equals(tradeDate, that.tradeDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tradeDate, companyId);
        }
    }
}