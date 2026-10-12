package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.StandardAgentOutput;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmMetrics;
import com.oraculum.llm.api.dto.LlmResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartMoneyAgentTest {

    @Mock
    private LlmRouterApi llmRouterApi;

    @Mock
    private PromptRegistry promptRegistry;

    @InjectMocks
    private SmartMoneyAgent smartMoneyAgent;

    @Mock
    private AgentContext ctx;

    @Mock
    private CompanyFactSheetData factSheetData;

    @Test
    void getName_shouldReturnSmartMoney() {
        assertThat(smartMoneyAgent.getName()).isEqualTo(AgentType.SMART_MONEY);
    }

    @Test
    void run_whenNoInstitutionalFlowData_shouldReturnDefaultOutputWithoutCallingLlm() {
        when(ctx.factSheetData()).thenReturn(factSheetData);
        when(ctx.ticker()).thenReturn("AAPL");
        when(factSheetData.getInstitutionalFlowHistoryJson()).thenReturn("[]");

        AgentOutput<StandardAgentOutput> output = smartMoneyAgent.run(ctx);

        assertThat(output).isNotNull();
        assertThat(output.tokens()).isEqualTo(0);
        assertThat(output.result().headline()).isEqualTo("No institutional 13F holding data available.");
        verifyNoInteractions(llmRouterApi);
        verifyNoInteractions(promptRegistry);
    }

    @Test
    void run_whenInstitutionalFlowDataPresent_shouldCallLlmAndReturnResult() {
        when(ctx.factSheetData()).thenReturn(factSheetData);
        when(ctx.ticker()).thenReturn("AAPL");
        com.oraculum.company.api.dto.CompanyDto companyDto = mock(com.oraculum.company.api.dto.CompanyDto.class);
        when(companyDto.sectorName()).thenReturn("Technology");
        when(ctx.company()).thenReturn(companyDto);
        when(factSheetData.getInstitutionalFlowHistoryJson()).thenReturn("[{\"total_inst_value_usd\": 1000000}]");
        when(factSheetData.getSectorRotationSummaryJson()).thenReturn("[{\"sector_name\": \"Technology\"}]");
        when(ctx.analysisFocus()).thenReturn("Growth focus");
        when(ctx.analysisDate()).thenReturn(LocalDate.of(2026, 6, 30));
        when(ctx.correlationId()).thenReturn(UUID.randomUUID());

        when(promptRegistry.getPrompt(PromptType.SMART_MONEY)).thenReturn("Prompt for {{ ticker }} with {{ company_sector }} {{ analysis_focus }} {{ institutional_flow_history }} {{ all_sectors_rotation }} {{ analysis_date }}");

        StandardAgentOutput expectedOutput = StandardAgentOutput.builder()
                .headline("Strong Tier 1 institutional accumulation in Q2 2026")
                .score(8.5)
                .confidence(new StandardAgentOutput.ConfidenceScore(null, null, 0.9))
                .confidenceReasons(List.of("High data quality"))
                .strengths(List.of("Buffett and Citadel added positions"))
                .weaknesses(List.of("Slight sector outflow"))
                .evidence(List.of())
                .metrics(Map.of("tier1_activity", "+$1.2B"))
                .summary("Institutions aggressively accumulated shares.")
                .build();

        LlmMetrics metrics = new LlmMetrics(null, null, 300, 150, 450, 100L);
        LlmResponse<StandardAgentOutput> response = new LlmResponse<>(expectedOutput, metrics);
        when(llmRouterApi.executeCall(any(LlmCallRequest.class))).thenReturn(response);

        AgentOutput<StandardAgentOutput> output = smartMoneyAgent.run(ctx);

        assertThat(output).isNotNull();
        assertThat(output.tokens()).isEqualTo(450);
        assertThat(output.result().headline()).isEqualTo("Strong Tier 1 institutional accumulation in Q2 2026");
        assertThat(output.result().score()).isEqualTo(8.5);
        verify(llmRouterApi).executeCall(any(LlmCallRequest.class));
    }
}
