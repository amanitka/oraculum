package com.oraculum.analyst.dto.valuation;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MultipleAnchorDto(
        @JsonProperty("multiple") Float multiple,
        @JsonProperty("label") String label
) {
}
