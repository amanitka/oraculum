package com.oraculum.company.domain;

import com.oraculum.company.api.domain.StatementVariant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_industry_financial_ratios")
@IdClass(IndustryFinancialRatiosId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndustryFinancialRatiosEntity {

    @Id
    @Column(name = "industry_name")
    private String industryName;

    @Id
    @Column(name = "variant")
    @Enumerated(EnumType.STRING)
    private StatementVariant variant;

    @Id
    @Column(name = "fiscal_year")
    private int fiscalYear;

    @Id
    @Column(name = "fiscal_period")
    private String fiscalPeriod;

    @Column(name = "return_on_equity")
    private Float returnOnEquity;

    @Column(name = "gross_margin")
    private Float grossMargin;

    @Column(name = "net_margin")
    private Float netMargin;

    @Column(name = "debt_to_equity")
    private Float debtToEquity;

    @Column(name = "current_ratio")
    private Float currentRatio;

    @Column(name = "operating_margin")
    private Float operatingMargin;

    @Column(name = "fcf_margin")
    private Float fcfMargin;

    @Column(name = "revenue_yoy_growth")
    private Float revenueYoyGrowth;
}
