package com.oraculum.harvester.provider.dto;

import com.oraculum.harvester.api.dto.EarningsEstimateDto;

import java.util.List;

public record AlphaVantageEarningsEstimatesResponse(
        String symbol,
        List<EarningsEstimateDto> estimates
) {
}

