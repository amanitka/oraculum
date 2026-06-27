package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MacroeconomicAgentOutput(
        @JsonProperty("macroeconomicContext") String macroeconomicContext
) {}
