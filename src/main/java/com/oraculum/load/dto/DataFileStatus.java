package com.oraculum.load.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record DataFileStatus(
        String ticker,
        String market,
        String source,
        @JsonProperty("file_type") String fileType,
        @JsonProperty("latest_processed_date") LocalDate latestProcessedDate,
        String status,
        @JsonProperty("extraction_status") String extractionStatus,
        String message
) {
}
