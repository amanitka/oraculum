package com.oraculum.analyst.agents.context;

import com.oraculum.analyst.domain.AgentType;
import com.oraculum.analyst.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyDto;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Map;

@Builder
public record AgentContext(CompanyDto company,
                           LocalDate requestDate,
                           StatementVariant defaultVariant,
                           int tokenBudget,
                           Map<AgentType, Object> priorOutputs) {
    public String ticker() {
        return company != null ? company.ticker() : null;
    }

    public String market() {
        return company != null ? company.market() : null;
    }
}