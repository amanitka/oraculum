package com.oraculum.analyst.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ExecutiveSummaryAgentOutput(@JsonProperty("verdict") String verdict,
                                          @JsonProperty("conviction") int conviction,
                                          @JsonProperty("thesis") String thesis,
                                          @JsonProperty("top_bull_points") List<String> topBullPoints,
                                          @JsonProperty("top_bear_points") List<String> topBearPoints,
                                          @JsonProperty("valuation_one_liner") String valuationOneLiner,
                                          @JsonProperty("what_would_change_this") String whatWouldChangeThis) {
}
