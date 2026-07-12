package com.oraculum.analyst.agent.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record SecEx991Response(
        @JsonProperty("summary") String summary,
        @JsonProperty("headline") String headline,
        @JsonProperty("reported_metrics") Map<String, String> reportedMetrics,
        @JsonProperty("guidance") Guidance guidance,
        @JsonProperty("key_announcements") List<String> keyAnnouncements,
        @JsonProperty("management_tone") String managementTone,
        @JsonProperty("sentiment_score") Double sentimentScore
) {
    public record Guidance(
            @JsonProperty("next_period_guidance") String nextPeriodGuidance,
            @JsonProperty("signal") String signal
    ) {
    }
}
