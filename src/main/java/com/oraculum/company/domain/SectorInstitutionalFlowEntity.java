package com.oraculum.company.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Immutable
@Table(name = "v_sector_institutional_flow")
@IdClass(SectorInstitutionalFlowEntity.SectorHoldingPeriodId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SectorInstitutionalFlowEntity {

    @Id
    @Column(name = "sector_name")
    private String sectorName;

    @Id
    @Column(name = "report_period")
    private LocalDate reportPeriod;

    @Column(name = "total_inst_value_usd")
    private Long totalInstValueUsd;

    @Column(name = "net_value_change_usd")
    private Long netValueChangeUsd;

    @Column(name = "capital_flow_usd")
    private Long capitalFlowUsd;

    @Column(name = "price_valuation_effect_usd")
    private Long priceValuationEffectUsd;

    @Column(name = "total_inst_shares")
    private Long totalInstShares;

    @Column(name = "net_shares_change")
    private Long netSharesChange;

    @Column(name = "companies_count")
    private Long companiesCount;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class SectorHoldingPeriodId implements Serializable {
        private String sectorName;
        private LocalDate reportPeriod;
    }
}
