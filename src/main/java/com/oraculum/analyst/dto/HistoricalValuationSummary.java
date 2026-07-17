package com.oraculum.analyst.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HistoricalValuationSummary(
        @JsonProperty("metric") String metric,
        @JsonProperty("current") Float current,
        @JsonProperty("avg_5y") Float avg5y,
        @JsonProperty("avg_10y") Float avg10y,
        @JsonProperty("percentile_10y") Integer percentile10y,
        @JsonProperty("min_10y") Float min10y,
        @JsonProperty("max_10y") Float max10y
) {}
