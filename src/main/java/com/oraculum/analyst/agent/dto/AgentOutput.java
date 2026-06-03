package com.oraculum.analyst.agent.dto;

public record AgentOutput<T>(T result,
                             int tokens) {
}
