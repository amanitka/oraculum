package com.oraculum.analyst.dto;

import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.AnalysisRecommendation;
import com.oraculum.analyst.api.domain.AnalysisStatus;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;

@Builder
public record CompanyAnalysisResult(UUID correlationId,
                                    String ticker,
                                    String market,
                                    LocalDate analysisDate,
                                    AnalysisStatus status,
                                    String reportMd,
                                    AnalysisOutlook outlook,
                                    AnalysisRecommendation recommendation,
                                    Integer conviction,
                                    List<String> keyDrivers,
                                    List<String> keyRisks,
                                    Map<String, Object> agentTrace,
                                    Integer tokenUsage,
                                    String error,
                                    ZonedDateTime createdAt,
                                    ZonedDateTime updatedAt) {
}