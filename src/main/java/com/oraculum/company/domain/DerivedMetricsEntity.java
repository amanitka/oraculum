package com.oraculum.company.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

@Entity
@Immutable
@Table(name = "v_derived_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DerivedMetricsEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "company_id")
    private int companyId;

    @Column(name = "currency")
    private String currency;

    @Column(name = "template")
    private String template;

    @Column(name = "variant")
    private String variant;

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

    @Column(name = "return_on_equity")
    private Float returnOnEquity;

    @Column(name = "net_margin")
    private Float netMargin;

    @Column(name = "revenue")
    private Float revenue;

    @Column(name = "net_income")
    private Float netIncome;
}