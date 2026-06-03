package com.oraculum.analyst.dto;

import com.oraculum.analyst.domain.StatementVariant;

import java.time.LocalDate;

public record CompanyAnalysisRequest(String ticker,
                                     String market,
                                     LocalDate requestDate,
                                     StatementVariant defaultVariant) {
}