package com.oraculum.analyst.agent.dto;

import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyDto;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.time.LocalDate;

@Builder
public record AgentContext(CompanyDto company,
                           CompanyFactSheetData factSheetData,
                           LocalDate analysisDate,
                           StatementVariant defaultStatementVariant,
                           int tokenBudget,
                           AgentWorkflowState state) {

    public String ticker() {
        return company.ticker();
    }

    public String market() {
        return company.market();
    }

    public @NonNull Integer companyId() {
        return company.id();
    }

    public StatementVariant getVariantFor(AgentType agentType) {
        var variants = state.getStatementVariants();
        if (variants != null && variants.containsKey(agentType)) {
            return variants.get(agentType);
        }
        return defaultStatementVariant != null ? defaultStatementVariant : StatementVariant.ANNUAL;
    }

    // Convenience delegates so agent code stays clean
    public String analysisFocus() {
        return state.getAnalysisFocus();
    }
}