package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.FundamentalsAgentOutput;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmMetrics;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.company.api.dto.CompanyDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FundamentalsAgentTest {

    @Mock
    private LlmRouterApi llmRouterApi;

    @Mock
    private PromptRegistry promptRegistry;

    @Mock
    private CompanyFactSheetData factSheetData;

    @InjectMocks
    private FundamentalsAgent agent;

    private AgentContext context;

    @BeforeEach
    void setUp() {
        CompanyDto companyDto = mock(CompanyDto.class);
        when(companyDto.ticker()).thenReturn("AAPL");
        com.oraculum.analyst.agent.dto.AgentWorkflowState state = mock(com.oraculum.analyst.agent.dto.AgentWorkflowState.class);
        when(state.getAnalysisFocus()).thenReturn("Focus on growth");
        context = AgentContext.builder()
                .correlationId(UUID.randomUUID())
                .company(companyDto)
                .analysisDate(LocalDate.now())
                .factSheetData(factSheetData)
                .state(state)
                .build();
    }

    @Test
    void run_returnsValidOutput() {
        when(promptRegistry.getPrompt(PromptType.FUNDAMENTALS)).thenReturn("Analyze {{ ticker }} focus: {{ analysis_focus }}");
        when(factSheetData.getIncomeStatementHistory(any())).thenReturn("IS_DATA");
        when(factSheetData.getBalanceSheetHistory(any())).thenReturn("BS_DATA");
        when(factSheetData.getCompanyFinancialRatios(any())).thenReturn("RATIO_DATA");
        when(factSheetData.getIncomeStatementHistory(any(), anyInt())).thenReturn("IS_DATA_A");
        when(factSheetData.getCompanyFinancialRatios(any(), anyInt())).thenReturn("RATIO_DATA_A");
        when(factSheetData.getLatestIndustryRatios(any())).thenReturn("IND_RATIO");

        FundamentalsAgentOutput outputData = mock(FundamentalsAgentOutput.class);
        LlmResponse<FundamentalsAgentOutput> llmResponse = new LlmResponse<>(outputData, new LlmMetrics(null, null, 100, 50, 150, 100L));
        
        when(llmRouterApi.executeCall(any(LlmCallRequest.class))).thenReturn((LlmResponse) llmResponse);

        AgentOutput<FundamentalsAgentOutput> result = agent.run(context);

        assertThat(result).isNotNull();
        assertThat(result.result()).isEqualTo(outputData);
        assertThat(result.tokens()).isEqualTo(150);

        ArgumentCaptor<LlmCallRequest> captor = ArgumentCaptor.forClass(LlmCallRequest.class);
        verify(llmRouterApi).executeCall(captor.capture());
        
        LlmCallRequest request = captor.getValue();
        assertThat(request.prompt()).contains("AAPL");
    }
}
