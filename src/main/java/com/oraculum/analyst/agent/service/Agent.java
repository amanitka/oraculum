package com.oraculum.analyst.agent.service;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.api.domain.AgentType;

public interface Agent<T> {
    AgentType getName();

    Class<T> getOutputModel();

    AgentOutput<T> run(AgentContext ctx);
}