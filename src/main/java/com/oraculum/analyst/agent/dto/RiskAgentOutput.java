package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RiskAgentOutput(@JsonProperty("key_risks") List<String> keyRisks,
                              @JsonProperty("summary") String summary) {
}
