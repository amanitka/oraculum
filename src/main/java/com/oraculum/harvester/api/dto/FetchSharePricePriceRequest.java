package com.oraculum.harvester.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class FetchSharePricePriceRequest extends HarvesterRequest {
    private final String market;
    private final String variant;

    @JsonProperty("from_date")
    private final String fromDate;

    @Override
    public String getRequestType() {
        return "fetch_share_price";
    }
}
