package com.oraculum.company.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Immutable
@Table(name = "v_daily_market_signals")
@IdClass(DailyMarketSignalEntity.DailyMarketSignalId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyMarketSignalEntity {

    @Id
    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @Id
    @Column(name = "company_id")
    private int companyId;

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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class DailyMarketSignalId implements Serializable {
        private LocalDate tradeDate;
        private int companyId;
    }
}