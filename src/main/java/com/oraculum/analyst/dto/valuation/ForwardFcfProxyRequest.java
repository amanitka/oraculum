package com.oraculum.analyst.dto.valuation;

import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import lombok.Builder;

import java.util.List;

@Builder
public record ForwardFcfProxyRequest(
        EarningsEstimateDto fy1,
        EarningsEstimateDto fy2,
        List<CompanyFinancialRatiosDto> annualRatios,
        CompanyFinancialRatiosDto ttmRatio,
        Double dilutedShares
) {
    public ForwardFcfProxyRequest {
        annualRatios = annualRatios != null ? annualRatios : List.of();
    }
}
