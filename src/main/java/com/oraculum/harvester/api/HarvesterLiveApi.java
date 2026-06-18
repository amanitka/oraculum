package com.oraculum.harvester.api;

import java.util.Optional;

public interface HarvesterLiveApi {
    /**
     * Fetches earnings estimates (EPS + revenue, annual + quarterly) for the given ticker.
     * Returns empty Optional if the daily API quota is exhausted.
     */
    Optional<String> fetchEarningsEstimates(String ticker);
}
