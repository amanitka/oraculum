package com.oraculum.analyst.agent.dto;

import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record AgentContext(UUID correlationId,
                           CompanyDto company,
                           CompanyFactSheetData factSheetData,
                           LocalDate analysisDate,
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

    public String analysisFocus() {
        return state.getAnalysisFocus();
    }

    public SharePriceSignalDto getLatestSignal() {
        return factSheetData.getLatestDailySignal();
    }

}