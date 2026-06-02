package com.oraculum.analyst.listener.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.analyst.domain.StatementVariant;

import java.time.LocalDate;
import java.util.UUID;

public record AnalyzeCompanyRequest(@JsonProperty("correlation_id") UUID correlationId,
                                    @JsonProperty("ticker") String ticker,
                                    @JsonProperty("market") String market,
                                    @JsonProperty("as_of") LocalDate asOf,
                                    @JsonProperty("default_variant") StatementVariant defaultVariant) {
}
