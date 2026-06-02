package com.oraculum.analyst.dto;

import com.oraculum.analyst.domain.StatementVariant;

import java.time.LocalDate;

public record AnalyzeCompanyRequest(String ticker,
                                    String market,
                                    LocalDate requestDate,
                                    StatementVariant defaultVariant) {
}