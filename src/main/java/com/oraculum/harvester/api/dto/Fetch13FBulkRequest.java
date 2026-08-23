package com.oraculum.harvester.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * Kafka request published to the Python harvester to trigger a quarterly
 * SEC 13F bulk ZIP download. The harvester downloads a single ZIP containing
 * all ~6,000 institutional filers for the given quarter and publishes
 * {@code sec_13f_holding} and {@code sec_13f_filer} DataFileReadyEvents.
 */
@Getter
@Builder
@Jacksonized
public class Fetch13FBulkRequest extends HarvesterRequest {

    @JsonProperty("year")
    private final int year;

    @JsonProperty("quarter")
    private final int quarter;

    @Override
    @JsonProperty("request_type")
    public String getRequestType() {
        return "fetch_13f_bulk";
    }
}
