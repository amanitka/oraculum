package com.oraculum.analyst.dto;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.SynthesizerAgentOutput;
import com.oraculum.analyst.api.dto.CompanyAnalysisRequestEvent;
import com.oraculum.analyst.api.dto.ExecutiveSummaryAgentOutput;
import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record CompanyAnalysisWorkflowResult(
        CompanyAnalysisRequestEvent request,
        AgentContext context,
        SynthesizerAgentOutput synthesizerOutput,
        ExecutiveSummaryAgentOutput executiveSummaryOutput,
        long startMs,
        ZonedDateTime now
) {
}
