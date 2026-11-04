package com.oraculum.analyst.dto.valuation;

import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder(toBuilder = true)
public record ValuationScenarioRequest(
        Float currentSharePrice,
        Double currentMarketCap,
        List<EarningsEstimateDto> estimates,
        LocalDate analysisDate,
        ReverseDcfDto reverseDcfResult,
        List<CompanyFinancialRatiosDto> quarterlyRatios,
        List<CompanyFinancialRatiosDto> annualRatios,
        CompanyFinancialRatiosDto ttmRatio,
        List<HistoricalValuationSummaryDto> historicalValuation,
        List<Float> explicitMultiples
) {
    public ValuationScenarioRequest {
        estimates = estimates != null ? estimates : List.of();
        quarterlyRatios = quarterlyRatios != null ? quarterlyRatios : List.of();
        annualRatios = annualRatios != null ? annualRatios : List.of();
        historicalValuation = historicalValuation != null ? historicalValuation : List.of();
        explicitMultiples = explicitMultiples != null ? explicitMultiples : List.of();
    }
}
