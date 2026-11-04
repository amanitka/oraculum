package com.oraculum.analyst.dto.valuation;

import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record ForwardReverseDcfRequest(
        Double marketCap,
        Float currentSharePrice,
        LocalDate analysisDate,
        EarningsEstimateDto fy1,
        EarningsEstimateDto fy2,
        List<EarningsEstimateDto> estimates,
        List<CompanyFinancialRatiosDto> annualRatios,
        CompanyFinancialRatiosDto ttmRatio
) {
    public ForwardReverseDcfRequest {
        estimates = estimates != null ? estimates : List.of();
        annualRatios = annualRatios != null ? annualRatios : List.of();
    }
}
