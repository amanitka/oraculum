package com.oraculum.analyst.dto;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.CompanyAnalysisRequestEvent;
import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record CompanyAnalysisWorkflowResult(
        CompanyAnalysisRequestEvent request,
        AgentContext context,
        AnalysisResult analysisResult,
        long startMs,
        ZonedDateTime now
) {
}
