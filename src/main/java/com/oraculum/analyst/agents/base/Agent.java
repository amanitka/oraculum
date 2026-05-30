package com.oraculum.analyst.agents.base;

import com.oraculum.analyst.agents.context.AgentContext;

public interface Agent<T> {
    String getName();
    Class<T> getOutputModel();
    AgentOutput<T> run(AgentContext ctx);
}
