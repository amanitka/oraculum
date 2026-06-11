package com.oraculum.analyst.agent.dto;

import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyDto;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Builder
public record AgentContext(CompanyDto company,
                           CompanyFactSheetData factSheetData,
                           LocalDate analysisDate,
                           StatementVariant defaultStatementVariant,
                           Map<AgentType, StatementVariant> statementVariants,
                           int tokenBudget,
                           Map<AgentType, Object> agentOutputs) {

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
        return defaultStatementVariant != null ? defaultStatementVariant : StatementVariant.ANNUAL;
    }

    public Map<AgentType, Object> getSpecialistAgentOutputs() {
        return agentOutputs().entrySet()
                .stream()
                .filter(entry -> entry.getKey().isSpecialist())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}