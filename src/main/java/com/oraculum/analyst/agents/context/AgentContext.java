package com.oraculum.analyst.agents.context;

import com.oraculum.analyst.agents.tools.DataTools;
import com.oraculum.analyst.domain.StatementTemplate;
import com.oraculum.analyst.domain.StatementVariant;
import com.oraculum.llm.api.LlmRouterApi;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.Map;

@Value
@Builder
public class AgentContext {
    String ticker;
    String market;
    Integer companyId;
    LocalDate asOf;
    StatementTemplate template;
    StatementVariant defaultVariant;
    DataTools tools;
    LlmRouterApi llm;
    int tokenBudget;
    Map<String, Object> priorOutputs;
}