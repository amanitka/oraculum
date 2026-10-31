package com.oraculum.company.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Immutable
@Table(name = "v_company_institutional_flow")
@IdClass(CompanyInstitutionalFlowEntity.CompanyHoldingPeriodId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyInstitutionalFlowEntity {

    @Id
    @Column(name = "company_id")
    private int companyId;

    @Column(name = "ticker")
    private String ticker;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "market")
    private String market;

    @Column(name = "sector_name")
    private String sectorName;

    @Column(name = "industry_name")
    private String industryName;

    @Column(name = "cusip")
    private String cusip;

    @Id
    @Column(name = "report_period")
    private LocalDate reportPeriod;

    @Column(name = "quarter_index")
    private long quarterIndex;

    @Column(name = "total_inst_value_usd")
    private Long totalInstValueUsd;

    @Column(name = "total_inst_shares")
    private Long totalInstShares;

    @Column(name = "total_holders_count")
    private Integer totalHoldersCount;

    @Column(name = "top_holders_names", columnDefinition = "text[]")
    private String[] topHoldersNames;

    @Column(name = "top5_holders_value_usd")
    private Long top5HoldersValueUsd;

    @Column(name = "top5_holders_pct")
    private Float top5HoldersPct;

    @Column(name = "put_value_usd")
    private Long putValueUsd;

    @Column(name = "call_value_usd")
    private Long callValueUsd;

    @Column(name = "put_call_ratio")
    private Float putCallRatio;

    @Column(name = "net_value_change_usd")
    private Long netValueChangeUsd;

    @Column(name = "net_shares_change")
    private Long netSharesChange;

    @Column(name = "shares_change_pct")
    private Float sharesChangePct;

    @Column(name = "capital_flow_usd")
    private Long capitalFlowUsd;

    @Column(name = "price_valuation_effect_usd")
    private Long priceValuationEffectUsd;

    @Column(name = "flag_incomplete_filer_sample")
    private Boolean flagIncompleteFilerSample;

    @Column(name = "funds_buying_count")
    private Integer fundsBuyingCount;

    @Column(name = "funds_selling_count")
    private Integer fundsSellingCount;

    @Column(name = "funds_new_count")
    private Integer fundsNewCount;

    @Column(name = "funds_closed_count")
    private Integer fundsClosedCount;

    @Column(name = "funds_unchanged_count")
    private Integer fundsUnchangedCount;

    @Column(name = "tier1_value_usd")
    private Long tier1ValueUsd;

    @Column(name = "tier1_shares")
    private Long tier1Shares;

    @Column(name = "tier1_holders_count")
    private Integer tier1HoldersCount;

    @Column(name = "tier1_net_value_change_usd")
    private Long tier1NetValueChangeUsd;

    @Column(name = "tier1_net_shares_change")
    private Long tier1NetSharesChange;

    @Column(name = "tier1_buyers_count")
    private Integer tier1BuyersCount;

    @Column(name = "tier1_sellers_count")
    private Integer tier1SellersCount;

    @Column(name = "tier1_new_count")
    private Integer tier1NewCount;

    @Column(name = "tier1_closed_count")
    private Integer tier1ClosedCount;

    @Column(name = "tier1_buyer_names", columnDefinition = "text[]")
    private String[] tier1BuyerNames;

    @Column(name = "tier1_seller_names", columnDefinition = "text[]")
    private String[] tier1SellerNames;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class CompanyHoldingPeriodId implements Serializable {
        private int companyId;
        private LocalDate reportPeriod;
    }
}
