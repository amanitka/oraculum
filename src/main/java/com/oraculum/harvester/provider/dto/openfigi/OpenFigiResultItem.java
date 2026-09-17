package com.oraculum.harvester.provider.dto.openfigi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenFigiResultItem(String figi,
                                 String name,
                                 String ticker,
                                 String exchCode,
                                 String compositeFIGI,
                                 String securityType,
                                 String marketSector,
                                 String shareClassFIGI,
                                 String securityDescription) {
}
