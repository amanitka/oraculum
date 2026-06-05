package com.oraculum.harvester.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class FetchNewsRequest extends HarvesterRequest {

    @JsonProperty("time_from")
    private final String timeFrom;

    @Override
    public String getRequestType() {
        return "fetch_news";
    }
}