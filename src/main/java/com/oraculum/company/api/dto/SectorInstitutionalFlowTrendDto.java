package com.oraculum.company.api.dto;

import com.oraculum.company.domain.SectorInstitutionalFlowEntity;

import java.time.LocalDate;

/**
 * Aggregated institutional 13F ownership and flow metrics per sector for a specific quarterly reporting period.
 */
public record SectorInstitutionalFlowTrendDto(
        String sectorName,
        LocalDate reportPeriod,
        Long totalInstValueUsd,
        Long netValueChangeUsd,
        Long capitalFlowUsd,
        Long priceValuationEffectUsd,
        Long totalInstShares,
        Long netSharesChange,
        Long companiesCount
) {
    public static SectorInstitutionalFlowTrendDto fromEntity(SectorInstitutionalFlowEntity entity) {
        if (entity == null) return null;
        return new SectorInstitutionalFlowTrendDto(
                entity.getSectorName(),
                entity.getReportPeriod(),
                entity.getTotalInstValueUsd(),
                entity.getNetValueChangeUsd(),
                entity.getCapitalFlowUsd(),
                entity.getPriceValuationEffectUsd(),
                entity.getTotalInstShares(),
                entity.getNetSharesChange(),
                entity.getCompaniesCount()
        );
    }

    public Float flowPct() {
        if (capitalFlowUsd != null && totalInstValueUsd != null && totalInstValueUsd > 0) {
            return (float) (capitalFlowUsd * 100.0 / totalInstValueUsd);
        }
        if (netValueChangeUsd == null || totalInstValueUsd == null || totalInstValueUsd <= 0) {
            return 0.0f;
        }
        return (float) (netValueChangeUsd * 100.0 / totalInstValueUsd);
    }
}
