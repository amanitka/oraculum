package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.AgentWorkflowState;
import com.oraculum.analyst.agent.dto.StandardAgentOutput;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.analyst.service.macro.SectorMacroIndicatorPolicy;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.economy.api.domain.MacroIndicator;
import com.oraculum.economy.api.dto.MacroSummaryDto;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmMetrics;
import com.oraculum.llm.api.dto.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MacroeconomicAgentTest {

    @Mock
    private LlmRouterApi llmRouterApi;

    @Mock
    private PromptRegistry promptRegistry;

    private MacroeconomicAgent agent;

    @BeforeEach
    void setUp() {
        agent = new MacroeconomicAgent(llmRouterApi, promptRegistry, new SectorMacroIndicatorPolicy());
    }

    @Test
    void getName_shouldReturnMacroeconomic() {
        assertThat(agent.getName()).isEqualTo(AgentType.MACROECONOMIC);
    }

    @Test
    void run_whenNoRelevantMacroeconomicData_shouldReturnDefaultOutput() {
        CompanyDto company = createCompany("NVDA", "Technology");
        // HOUST is excluded for Technology sector
        MacroSummaryDto housingOnly = createMacro(MacroIndicator.HOUST);
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .company(company)
                .macroeconomicSummary(List.of(housingOnly))
                .build();

        AgentOutput<StandardAgentOutput> output = agent.run(createContext(company, factSheetData, null));

        assertThat(output).isNotNull();
        assertThat(output.tokens()).isZero();
        assertThat(output.result().headline()).isEqualTo("No macroeconomic data available.");
        verifyNoInteractions(llmRouterApi);
    }

    @Test
    void run_whenDataPresent_shouldFilterBySectorAndCallLlm() {
        CompanyDto company = createCompany("NVDA", "Technology");
        MacroSummaryDto dgorder = createMacro(MacroIndicator.DGORDER);
        MacroSummaryDto housing = createMacro(MacroIndicator.HOUST);
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .company(company)
                .macroeconomicSummary(List.of(dgorder, housing))
                .build();

        when(promptRegistry.getPrompt(PromptType.MACROECONOMIC)).thenReturn(
                "Macro prompt: ticker={{ ticker }}, date={{ analysis_date }}, summary={{ macroeconomic_summary }}"
        );

        StandardAgentOutput expectedOutput = StandardAgentOutput.builder()
                .headline("Macro tailwinds supportive")
                .score(8.0)
                .build();
        LlmMetrics metrics = new LlmMetrics(null, null, 100, 200, 300, 10L);
        when(llmRouterApi.executeCall(any(LlmCallRequest.class)))
                .thenReturn(new LlmResponse<>(expectedOutput, metrics));

        AgentOutput<StandardAgentOutput> output = agent.run(createContext(company, factSheetData, null));

        assertThat(output).isNotNull();
        assertThat(output.tokens()).isEqualTo(300);
        assertThat(output.result().headline()).isEqualTo("Macro tailwinds supportive");

        ArgumentCaptor<LlmCallRequest> captor = ArgumentCaptor.forClass(LlmCallRequest.class);
        verify(llmRouterApi).executeCall(captor.capture());
        assertThat(captor.getValue().prompt())
                .contains("ticker=NVDA")
                .contains("DGORDER")
                .doesNotContain("HOUST");
    }

    @Test
    void run_whenCriticFeedbackPresent_shouldAppendFeedbackToPrompt() {
        CompanyDto company = createCompany("NVDA", "Technology");
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .company(company)
                .macroeconomicSummary(List.of(createMacro(MacroIndicator.DGORDER)))
                .build();

        AgentWorkflowState state = new AgentWorkflowState();
        state.setCriticFeedback(Map.of(AgentType.MACROECONOMIC, "Consider interest rate impact"));

        when(promptRegistry.getPrompt(PromptType.MACROECONOMIC)).thenReturn("Base prompt {{ macroeconomic_summary }}");

        StandardAgentOutput expectedOutput = StandardAgentOutput.builder().headline("Revised").score(7.0).build();
        when(llmRouterApi.executeCall(any(LlmCallRequest.class)))
                .thenReturn(new LlmResponse<>(expectedOutput, new LlmMetrics(null, null, 50, 50, 100, 5L)));

        AgentOutput<StandardAgentOutput> output = agent.run(createContext(company, factSheetData, state));

        assertThat(output).isNotNull();
        ArgumentCaptor<LlmCallRequest> captor = ArgumentCaptor.forClass(LlmCallRequest.class);
        verify(llmRouterApi).executeCall(captor.capture());
        assertThat(captor.getValue().prompt())
                .contains("### CRITIC FEEDBACK (Correction Required)")
                .contains("Consider interest rate impact");
    }

    private MacroSummaryDto createMacro(MacroIndicator indicator) {
        return new MacroSummaryDto(
                indicator, indicator.name(), LocalDate.of(2026, 10, 1),
                100.0, 98.0, 2.0, 200.0, 90.0, 105.0, 97.5, 1.5
        );
    }

    private CompanyDto createCompany(String ticker, String sector) {
        return new CompanyDto(
                1, ticker, "US", "Company " + ticker,
                "100", "Semiconductors", sector,
                "US0000000000", "Description", 10000L,
                "USD", "0000000000", "000000000", null
        );
    }

    private AgentContext createContext(CompanyDto company, CompanyFactSheetData factSheetData, AgentWorkflowState state) {
        return AgentContext.builder()
                .correlationId(UUID.randomUUID())
                .company(company)
                .analysisDate(LocalDate.of(2026, 10, 28))
                .factSheetData(factSheetData)
                .state(state)
                .build();
    }
}
