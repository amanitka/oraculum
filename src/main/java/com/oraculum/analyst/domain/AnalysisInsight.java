package com.oraculum.analyst.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AnalysisInsight {
    @JsonProperty("insight_summary")
    private String insightSummary;
    @JsonProperty("supporting_data_points")
    private List<String> supportingDataPoints;
}