package com.oraculum.analyst.agents.base;

import com.oraculum.analyst.agents.context.AgentContext;
import com.oraculum.analyst.domain.AgentType;

public interface Agent<T> {
    AgentType getName();
    Class<T> getOutputModel();
    AgentOutput<T> run(AgentContext ctx);
}