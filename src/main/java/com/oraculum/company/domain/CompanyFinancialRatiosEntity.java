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

    @Column(name = "return_on_equity")
    private Float returnOnEquity;

    @Column(name = "net_margin")
    private Float netMargin;

    @Column(name = "revenue")
    private Float revenue;

    @Column(name = "net_income")
    private Float netIncome;
}