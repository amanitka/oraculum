package com.oraculum.analyst.agents.base;

public record AgentOutput<T>(T result,
                             int tokens) {
}
