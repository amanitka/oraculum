package com.oraculum.company.api.event;

import java.time.LocalDate;
import java.util.List;

public record TickerDocumentLoadEvent(
        List<DocumentStatus> fileStatuses
) {
    public record DocumentStatus(
            String ticker,
            String market,
            String source,
            String fileType,
            LocalDate latestProcessedDate,
            String status,
            String extractionStatus,
            String message
    ) {
    }
}
