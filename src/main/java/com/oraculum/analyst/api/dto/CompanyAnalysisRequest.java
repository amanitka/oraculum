package com.oraculum.analyst.api.dto;

import com.oraculum.company.api.domain.StatementVariant;

import java.time.LocalDate;
import java.util.UUID;

public record CompanyAnalysisRequest(UUID correlationId,
                                     Integer companyId,
                                     String ticker,
                                     String market,
                                     LocalDate analysisDate,
                                     StatementVariant statementVariant) {
}