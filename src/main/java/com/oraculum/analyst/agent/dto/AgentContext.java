package com.oraculum.analyst.agent.dto;

import com.oraculum.analyst.domain.AgentType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyDto;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.time.LocalDate;
import java.util.Map;

@Builder
public record AgentContext(CompanyDto company,
                           CompanyFactSheetData factSheetData,
                           LocalDate analysisDate,
                           StatementVariant defaultStatementVariant,
                           Map<AgentType, StatementVariant> statementVariants,
                           int tokenBudget,
                           Map<AgentType, Object> priorOutputs) {
    public String ticker() {
        return company != null ? company.ticker() : null;
    }

    public String market() {
        return company != null ? company.market() : null;
    }

    public @NonNull Integer companyId() {
        return company.id();
    }

    public StatementVariant getVariantFor(AgentType agentType) {
        if (statementVariants != null && statementVariants.containsKey(agentType)) {
            return statementVariants.get(agentType);
        }
        return defaultStatementVariant;
    }
}