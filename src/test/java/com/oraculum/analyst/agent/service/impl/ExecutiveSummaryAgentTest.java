package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.AgentWorkflowState;
import com.oraculum.analyst.agent.dto.SynthesizerAgentOutput;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.AnalysisRecommendation;
import com.oraculum.analyst.api.dto.ExecutiveSummaryAgentOutput;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExecutiveSummaryAgentTest {

    @Mock
    private LlmRouterApi llmRouterApi;

    @Mock
    private PromptRegistry promptRegistry;

    @InjectMocks
    private ExecutiveSummaryAgent agent;

    private AgentContext context;
    private AgentWorkflowState state;

    @BeforeEach
    void setUp() {
        CompanyDto companyDto = mock(CompanyDto.class);
        when(companyDto.ticker()).thenReturn("NVDA");
        state = new AgentWorkflowState();
        context = AgentContext.builder()
                .correlationId(UUID.randomUUID())
                .company(companyDto)
                .analysisDate(LocalDate.now())
                .state(state)
                .build();
    }

    @Test
    void getName_returnsExecutiveSummary() {
        assertThat(agent.getName()).isEqualTo(AgentType.EXECUTIVE_SUMMARY);
    }

    @Test
    void run_throwsException_whenSynthesizerOutputMissing() {
        assertThatThrownBy(() -> agent.run(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SynthesizerAgentOutput must be present");
    }

    @Test
    void run_returnsValidOutput_whenSynthesizerOutputPresent() {
        SynthesizerAgentOutput synthOutput = new SynthesizerAgentOutput(
                "## Executive Summary\nStrong growth.",
                AnalysisOutlook.BULLISH,
                AnalysisRecommendation.BUY,
                4,
                List.of("Strong AI capex"),
                List.of("Export control risks")
        );
        state.putAgentOutput(AgentType.SYNTHESIZER, synthOutput);

        String template = "Ticker: {{ ticker }}, Outlook: {{ outlook }}, Rec: {{ recommendation }}, Conv: {{ conviction }}, Report: {{ report }}";
        when(promptRegistry.getPrompt(PromptType.EXECUTIVE_SUMMARY)).thenReturn(template);

        ExecutiveSummaryAgentOutput expectedOutput = new ExecutiveSummaryAgentOutput(
                "BUY", 4, "NVDA has strong AI growth.",
                List.of("70% TTM growth"), List.of("Export restrictions"),
                "P/E of 31x", "Data center growth < 25%"
        );
        LlmResponse<ExecutiveSummaryAgentOutput> llmResponse = new LlmResponse<>(
                expectedOutput, new LlmMetrics(null, null, 100, 50, 150, 100L)
        );
        when(llmRouterApi.executeCall(any(LlmCallRequest.class))).thenReturn(llmResponse);

        AgentOutput<ExecutiveSummaryAgentOutput> result = agent.run(context);

        assertThat(result).isNotNull();
        assertThat(result.result()).isEqualTo(expectedOutput);
        assertThat(result.tokens()).isEqualTo(150);

        ArgumentCaptor<LlmCallRequest> captor = ArgumentCaptor.forClass(LlmCallRequest.class);
        verify(llmRouterApi).executeCall(captor.capture());
        LlmCallRequest request = captor.getValue();

        assertThat(request.prompt()).contains("Ticker: NVDA");
        assertThat(request.prompt()).contains("Outlook: BULLISH");
        assertThat(request.prompt()).contains("Rec: BUY");
        assertThat(request.prompt()).contains("Conv: 4");
    }
}
