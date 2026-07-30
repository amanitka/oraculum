package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.analyst.api.domain.TrendDirection;

public record ChangeDelta(
        @JsonProperty("category") String category,
        @JsonProperty("trend") TrendDirection trend,
        @JsonProperty("description") String description
) {}
