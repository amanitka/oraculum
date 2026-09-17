package com.oraculum.harvester.provider.dto.openfigi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.util.CollectionUtils;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenFigiMappingResponse(List<OpenFigiResultItem> data,
                                      String warning,
                                      String error) {
    public boolean hasData() {
        return !CollectionUtils.isEmpty(data);
    }
}
