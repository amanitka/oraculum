package com.oraculum.analyst.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CashFlowOutput {
    @JsonProperty("cash_generation_analysis")
    private String cashGenerationAnalysis;

    @JsonProperty("capex_intensity_analysis")
    private String capexIntensityAnalysis;

    @JsonProperty("summary")
    private String summary;
}