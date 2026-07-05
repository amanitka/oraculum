package com.oraculum.harvester.provider.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public record FredResponse(
        @JsonProperty("realtime_start") String realtimeStart,
        @JsonProperty("realtime_end") String realtimeEnd,
        @JsonProperty("observation_start") String observationStart,
        @JsonProperty("observation_end") String observationEnd,
        @JsonProperty("units") String units,
        @JsonProperty("output_type") int outputType,
        @JsonProperty("file_type") String fileType,
        @JsonProperty("order_by") String orderBy,
        @JsonProperty("sort_order") String sortOrder,
        @JsonProperty("count") int count,
        @JsonProperty("offset") int offset,
        @JsonProperty("limit") int limit,
        @JsonProperty("observations") List<FredObservation> observations
) {
    public record FredObservation(
            @JsonProperty("realtime_start") LocalDate realtimeStart,
            @JsonProperty("realtime_end") LocalDate realtimeEnd,
            @JsonProperty("date") LocalDate date,
            @JsonProperty("value") String value
    ) {
    }
}
