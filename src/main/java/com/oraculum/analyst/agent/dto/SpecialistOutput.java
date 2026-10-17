package com.oraculum.analyst.agent.dto;

import com.oraculum.analyst.agent.dto.StandardAgentOutput.ConfidenceScore;

import java.util.List;
import java.util.Map;

public sealed interface SpecialistOutput permits StandardAgentOutput, RiskAgentOutput {

    String headline();

    double score();

    ConfidenceScore confidence();

    List<String> confidenceReasons();

    List<AgentEvidence> evidence();

    Map<String, Object> metrics();

    String summary();

    SpecialistOutput withConfidence(ConfidenceScore confidence, List<String> reasons);
}
