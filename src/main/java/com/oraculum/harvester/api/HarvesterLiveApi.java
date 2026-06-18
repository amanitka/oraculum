package com.oraculum.harvester.api;

import com.oraculum.harvester.api.dto.EarningsEstimateDto;

import java.util.List;
import java.util.Optional;

public interface HarvesterLiveApi {
    /**
     * Fetches earnings estimates (EPS + revenue, annual + quarterly) for the given ticker.
     * Returns empty Optional if the daily API quota is exhausted.
     */
    Optional<List<EarningsEstimateDto>> fetchEarningsEstimates(String ticker);
}
