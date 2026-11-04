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
import com.oraculum.company.api.dto.InsiderTransactionByPersonDto;
import com.oraculum.company.api.dto.InsiderTransactionSummaryDto;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsiderTransactionAgentTest {

    @Mock
    private LlmRouterApi llmRouterApi;

    @Mock
    private PromptRegistry promptRegistry;

    @InjectMocks
    private InsiderTransactionAgent agent;

    private CompanyDto companyDto;

    @BeforeEach
    void setUp() {
        companyDto = new CompanyDto(
                1, "NVDA", "US", "Nvidia Corp", "100", "Semiconductors", "Technology",
                "US0000000000", "Description", 10000L, "USD", "0000000000", "000000000", null
        );
    }

    @Test
    void getName_shouldReturnInsiderTransaction() {
        assertThat(agent.getName()).isEqualTo(AgentType.INSIDER_TRANSACTION);
    }

    @Test
    void run_whenNoInsiderTransactions_shouldReturnDefaultOutput() {
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .company(companyDto)
                .insiderTransactionsByPerson(List.of())
                .build();

        AgentContext context = createContext(factSheetData);
        AgentOutput<StandardAgentOutput> output = agent.run(context);

        assertThat(output.result()).isNotNull();
        assertThat(output.result().headline()).isEqualTo("No insider transactions available.");
        assertThat(output.tokens()).isZero();
        verifyNoInteractions(llmRouterApi);
    }

    @Test
    void run_whenDataPresent_shouldExecuteLlmCallWithReplacedPrompt() {
        InsiderTransactionByPersonDto personDto = InsiderTransactionByPersonDto.builder()
                .ticker("NVDA")
                .insiderName("Jensen Huang")
                .title("CEO")
                .txCount3m(2L)
                .build();
        InsiderTransactionSummaryDto summaryDto = InsiderTransactionSummaryDto.builder()
                .ticker("NVDA")
                .hasClusterBuy(false)
                .build();
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .company(companyDto)
                .insiderTransactionsByPerson(List.of(personDto))
                .insiderTransactionSummary(summaryDto)
                .build();

        when(promptRegistry.getPrompt(PromptType.INSIDER_TRANSACTION)).thenReturn(
                "Prompt ticker: {{ ticker }}, byPerson: {{ insider_transactions_by_person }}, summary: {{ insider_summary }}"
        );

        StandardAgentOutput expectedOutput = StandardAgentOutput.builder()
                .headline("Routine Rule 10b5-1 sales neutral")
                .score(5.5)
                .build();
        LlmMetrics metrics = new LlmMetrics(null, null, 150, 50, 200, 1000L);
        when(llmRouterApi.executeCall(any(LlmCallRequest.class)))
                .thenReturn(new LlmResponse<>(expectedOutput, metrics));

        AgentOutput<StandardAgentOutput> result = agent.run(createContext(factSheetData));

        assertThat(result.result()).isEqualTo(expectedOutput);
        assertThat(result.tokens()).isEqualTo(200);

        ArgumentCaptor<LlmCallRequest> captor = ArgumentCaptor.forClass(LlmCallRequest.class);
        verify(llmRouterApi).executeCall(captor.capture());
        String sentPrompt = captor.getValue().prompt();
        assertThat(sentPrompt).contains("Prompt ticker: NVDA");
        assertThat(sentPrompt).contains("Jensen Huang");
    }

    private AgentContext createContext(CompanyFactSheetData factSheetData) {
        return AgentContext.builder()
                .correlationId(UUID.randomUUID())
                .company(companyDto)
                .analysisDate(LocalDate.of(2026, 10, 15))
                .factSheetData(factSheetData)
                .state(new AgentWorkflowState())
                .build();
    }
}
