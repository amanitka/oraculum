package com.oraculum.company.api;

import java.time.LocalDate;
import java.util.List;

public interface CompanySecApi {

    /**
     * Returns a list of distinct CUSIP identifiers found across institutional 13F holdings.
     *
     * @return list of unique CUSIP strings
     */
    List<String> getDistinctHoldingCusips();

    /**
     * Checks whether institutional 13F holdings have already been loaded for the given report period.
     *
     * @param reportPeriod quarter-end date (e.g., 2026-06-30)
     * @return {@code true} if at least one holding record exists for the period
     */
    boolean hasHoldingsForPeriod(LocalDate reportPeriod);
}
