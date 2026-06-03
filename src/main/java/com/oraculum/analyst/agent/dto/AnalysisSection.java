package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AnalysisSection(@JsonProperty("section_title") String sectionTitle,
                              @JsonProperty("commentary") String commentary,
                              @JsonProperty("key_insights") List<AnalysisInsight> keyInsights) {
}