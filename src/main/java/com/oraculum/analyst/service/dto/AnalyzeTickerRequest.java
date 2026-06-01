package com.oraculum.analyst.service.dto;

import com.oraculum.analyst.domain.StatementVariant;
import java.time.LocalDate;

public record AnalyzeTickerRequest(
    String ticker,
    String market,
    LocalDate asOf,
    StatementVariant defaultVariant
) {
}