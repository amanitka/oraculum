package com.oraculum.analyst.agent.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record SecMdResponse(
        @JsonProperty("summary") String summary,
        @JsonProperty("primary_revenue_drivers") List<String> primaryRevenueDrivers,
        @JsonProperty("margin_trends") String marginTrends,
        @JsonProperty("segment_highlights") List<String> segmentHighlights,
        @JsonProperty("guidance_signal") String guidanceSignal,
        @JsonProperty("management_outlook") String managementOutlook,
        @JsonProperty("capital_allocation") List<String> capitalAllocation,
        @JsonProperty("key_metrics") Map<String, Double> keyMetrics,
        @JsonProperty("sentiment_score") Double sentimentScore
) {
}
