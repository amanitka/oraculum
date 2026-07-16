package com.oraculum.analyst.api.dto;


import com.oraculum.company.api.dto.TickerKeyDto;
import java.time.LocalDate;
import java.util.UUID;

public record CompanyAnalysisRequestEvent(UUID correlationId,
                                          Integer companyId,
                                          TickerKeyDto ticker,
                                          LocalDate analysisDate,
                                          String analysisFocus,
                                          Long requestedBy) {
}
