package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.AgentWorkflowState;
import com.oraculum.analyst.agent.dto.StandardAgentOutput;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmMetrics;
import com.oraculum.llm.api.dto.LlmResponse;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InsiderTransactionAgentTest {

    @Mock
    private LlmRouterApi llmRouterApi;

    @Mock
    private PromptRegistry promptRegistry;

    @Mock
    private CompanyFactSheetData factSheetData;

    @InjectMocks
    private InsiderTransactionAgent agent;

    private AgentContext context;

    @BeforeEach
    void setUp() {
        CompanyDto companyDto = mock(CompanyDto.class);
        when(companyDto.ticker()).thenReturn("NVDA");
        AgentWorkflowState state = mock(AgentWorkflowState.class);

        context = AgentContext.builder()
                .correlationId(UUID.randomUUID())
                .company(companyDto)
                .analysisDate(LocalDate.of(2026, 10, 15))
                .factSheetData(factSheetData)
                .state(state)
                .build();
    }

    @Test
    void getName_shouldReturnInsiderTransaction() {
        assertThat(agent.getName()).isEqualTo(AgentType.INSIDER_TRANSACTION);
    }

    @Test
    void run_whenNoInsiderTransactions_shouldReturnDefaultOutput() {
        when(factSheetData.getInsiderTransactionsByPerson()).thenReturn("[]");

        AgentOutput<StandardAgentOutput> output = agent.run(context);

        assertThat(output.result()).isNotNull();
        assertThat(output.result().headline()).isEqualTo("No insider transactions available.");
        assertThat(output.tokens()).isZero();
    }

    @Test
    void run_whenDataPresent_shouldExecuteLlmCallWithReplacedPrompt() {
        when(factSheetData.getInsiderTransactionsByPerson()).thenReturn("[{\"insiderName\":\"Jensen Huang\"}]");
        when(factSheetData.getInsiderTransactionSummary()).thenReturn("{\"hasClusterBuy\":false}");
        when(promptRegistry.getPrompt(PromptType.INSIDER_TRANSACTION)).thenReturn(
                "Prompt ticker: {{ ticker }}, byPerson: {{ insider_transactions_by_person }}, summary: {{ insider_summary }}"
        );

        StandardAgentOutput expectedOutput = mock(StandardAgentOutput.class);
        LlmMetrics metrics = new LlmMetrics(null, null, 150, 50, 200, 1000L);
        LlmResponse<StandardAgentOutput> llmResponse = new LlmResponse<>(expectedOutput, metrics);
        when(llmRouterApi.executeCall(any(LlmCallRequest.class)))
                .thenReturn(llmResponse);

        AgentOutput<StandardAgentOutput> result = agent.run(context);

        assertThat(result.result()).isEqualTo(expectedOutput);
        assertThat(result.tokens()).isEqualTo(200);

        ArgumentCaptor<LlmCallRequest> captor = ArgumentCaptor.forClass(LlmCallRequest.class);
        verify(llmRouterApi).executeCall(captor.capture());
        String sentPrompt = captor.getValue().prompt();
        assertThat(sentPrompt).contains("Prompt ticker: NVDA");
        assertThat(sentPrompt).contains("byPerson: [{\"insiderName\":\"Jensen Huang\"}]");
    }
}
