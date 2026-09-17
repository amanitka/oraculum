package com.oraculum.company.api;

import java.util.List;

public interface CompanySecApi {

    /**
     * Returns a list of distinct CUSIP identifiers found across institutional 13F holdings.
     *
     * @return list of unique CUSIP strings
     */
    List<String> getDistinctHoldingCusips();
}
