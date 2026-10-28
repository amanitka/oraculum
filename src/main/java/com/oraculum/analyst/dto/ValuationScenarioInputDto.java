package com.oraculum.analyst.dto;

import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder(toBuilder = true)
public record ValuationScenarioInputDto(
        Float currentSharePrice,
        List<EarningsEstimateDto> estimates,
        LocalDate analysisDate,
        ReverseDcfDto reverseDcfResult,
        List<CompanyFinancialRatiosDto> quarterlyRatios,
        List<HistoricalValuationSummaryDto> historicalValuation,
        List<Float> explicitMultiples
) {
    public ValuationScenarioInputDto {
        estimates = estimates != null ? estimates : List.of();
        quarterlyRatios = quarterlyRatios != null ? quarterlyRatios : List.of();
        historicalValuation = historicalValuation != null ? historicalValuation : List.of();
        explicitMultiples = explicitMultiples != null ? explicitMultiples : List.of();
    }
}
