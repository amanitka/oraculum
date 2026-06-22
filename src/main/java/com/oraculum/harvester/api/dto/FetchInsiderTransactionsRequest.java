package com.oraculum.harvester.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class FetchInsiderTransactionsRequest extends HarvesterRequest {

    @JsonProperty("max_filing_date")
    private final String maxFilingDate;

    @Override
    @JsonProperty("request_type")
    public String getRequestType() {
        return "fetch_insider_transactions";
    }
}
