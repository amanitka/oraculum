package com.oraculum.analyst.listener.message;

import com.oraculum.company.api.domain.StatementVariant;

import java.time.LocalDate;
import java.util.UUID;

public record AnalyzeCompanyRequest(UUID correlationId,
                                    Integer companyId,
                                    String ticker,
                                    String market,
                                    StatementVariant statementVariant,
                                    LocalDate analysisDate) {
}
