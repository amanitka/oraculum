package com.oraculum.analyst.api.dto;


import java.time.LocalDate;
import java.util.UUID;

public record CompanyAnalysisRequestEvent(UUID correlationId,
                                          Integer companyId,
                                          String ticker,
                                          String market,
                                          LocalDate analysisDate,
                                          String analysisFocus,
                                          Long requestedBy) {
}
