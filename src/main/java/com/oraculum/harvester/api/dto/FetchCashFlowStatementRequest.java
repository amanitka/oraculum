package com.oraculum.harvester.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class FetchCashFlowStatementRequest extends HarvesterRequest {
    private final String market;
    private final String variant;
    private final List<String> templates;

    @Override
    public String getRequestType() {
        return "fetch_cash_flow_statement";
    }
}
