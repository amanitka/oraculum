package com.oraculum.analyst.dto;

import com.oraculum.analyst.api.domain.AnalysisStatus;
import com.oraculum.analyst.api.dto.AnalysisResult;
import lombok.Builder;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
public record CompanyAnalysisResult(UUID correlationId,
                                    String ticker,
                                    String market,
                                    LocalDate analysisDate,
                                    AnalysisStatus status,
                                    AnalysisResult analysisResult,
                                    Map<String, Object> agentTrace,
                                    Integer tokenUsage,
                                    String error,
                                    ZonedDateTime createdAt,
                                    ZonedDateTime updatedAt) {
}
