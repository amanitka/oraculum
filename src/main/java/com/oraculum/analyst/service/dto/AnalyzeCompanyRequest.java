package com.oraculum.analyst.service.dto;

import com.oraculum.analyst.domain.StatementVariant;

import java.time.LocalDate;

public record AnalyzeCompanyRequest(String ticker,
                                    String market,
                                    LocalDate requestDate,
                                    StatementVariant defaultVariant) {
}