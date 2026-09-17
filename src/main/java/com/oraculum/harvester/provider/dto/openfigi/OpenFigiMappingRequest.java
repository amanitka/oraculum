package com.oraculum.harvester.provider.dto.openfigi;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenFigiMappingRequest(String idType, String idValue, String exchCode) {
    public static OpenFigiMappingRequest ofCusip(String cusip, String exchCode) {
        return new OpenFigiMappingRequest("ID_CUSIP", cusip, exchCode);
    }
}
