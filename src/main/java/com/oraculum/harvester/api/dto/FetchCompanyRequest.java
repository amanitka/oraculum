package com.oraculum.harvester.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class FetchCompanyRequest extends HarvesterRequest {
    private final String market;

    @Override
    public String getRequestType() {
        return "fetch_company";
    }
}
