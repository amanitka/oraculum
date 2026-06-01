package com.oraculum.analyst.agents.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.analyst.domain.AnalysisOutlook;
import com.oraculum.analyst.domain.AnalysisRecommendation;

import java.util.List;

public record SynthesizerAgentOutput(
    @JsonProperty("report_md") String reportMd,
    @JsonProperty("outlook") AnalysisOutlook outlook,
    @JsonProperty("recommendation") AnalysisRecommendation recommendation,
    @JsonProperty("conviction") int conviction,
    @JsonProperty("key_drivers") List<String> keyDrivers,
    @JsonProperty("key_risks") List<String> keyRisks
) {
}