package com.oraculum.harvester.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class FetchIndustryRequest extends HarvesterRequest {
    @Override
    public String getRequestType() {
        return "fetch_industry";
    }
}
