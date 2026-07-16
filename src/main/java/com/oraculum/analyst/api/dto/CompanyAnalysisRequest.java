package com.oraculum.analyst.api.dto;

import com.oraculum.company.api.dto.TickerKeyDto;
import java.time.LocalDate;
import java.util.UUID;

public record CompanyAnalysisRequest(
        UUID correlationId,
        Integer companyId,
        TickerKeyDto ticker,
        LocalDate analysisDate,
        String analysisFocus
) {
}
