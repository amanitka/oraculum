package com.oraculum.analyst.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AnalysisSection {
    @JsonProperty("section_title")
    private String sectionTitle;
    @JsonProperty("commentary")
    private String commentary;
    @JsonProperty("key_insights")
    private List<AnalysisInsight> keyInsights;
}