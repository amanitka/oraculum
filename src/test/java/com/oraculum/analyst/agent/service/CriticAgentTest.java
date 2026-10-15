package com.oraculum.analyst.agent.service;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.AgentWorkflowState;
import com.oraculum.analyst.agent.dto.CriticAgentOutput;
import com.oraculum.analyst.agent.dto.CriticAgentOutput.Contradiction;
import com.oraculum.analyst.agent.dto.CriticAgentOutput.CorrectionType;
import com.oraculum.analyst.agent.dto.CriticAgentOutput.Resolution;
import com.oraculum.analyst.agent.service.impl.CriticAgent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.service.PreCriticValidationService;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmMetrics;
import com.oraculum.llm.api.dto.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CriticAgentTest {

    @Mock
    private LlmRouterApi llmRouterApi;

    @Mock
    private PromptRegistry promptRegistry;

    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private PreCriticValidationService preCriticValidationService;

    @InjectMocks
    private CriticAgent agent;

    private AgentContext context;

    @BeforeEach
    void setUp() {
        CompanyDto company = mock(CompanyDto.class);
        when(company.ticker()).thenReturn("NVDA");

        AgentWorkflowState state = new AgentWorkflowState();
        context = AgentContext.builder()
                .correlationId(UUID.randomUUID())
                .company(company)
                .analysisDate(LocalDate.now())
                .state(state)
                .build();

        when(promptRegistry.getPrompt(PromptType.CRITIC)).thenReturn("Prompt for {{ ticker }} checks: {{ deterministic_checks }}");
        try {
            when(jsonMapper.writeValueAsString(any())).thenReturn("[]");
        } catch (Exception ignored) {
        }
    }

    @Test
    void getName_returnsCritic() {
        assertThat(agent.getName()).isEqualTo(AgentType.CRITIC);
    }

    @Test
    void run_mergesDeterministicChecksAndAppendsReruns() {
        Contradiction deterministic = new Contradiction(
                "Reverse DCF growth hurdle mismatch",
                List.of("VALUATION"),
                CorrectionType.REASONING_ERROR,
                Resolution.RERUN_APPLIED
        );
        when(preCriticValidationService.validate(context)).thenReturn(List.of(deterministic));

        CriticAgentOutput llmResult = new CriticAgentOutput(
                List.of(),
                true,
                List.of(),
                "Adversarial counter-thesis: Hyperscaler capex digestion poses sharp margin risk."
        );
        LlmResponse<CriticAgentOutput> response = new LlmResponse<>(
                llmResult, new LlmMetrics(null, null, 150, 50, 200, 120L)
        );
        when(llmRouterApi.executeCall(any(LlmCallRequest.class))).thenReturn(response);

        AgentOutput<CriticAgentOutput> output = agent.run(context);

        assertThat(output).isNotNull();
        CriticAgentOutput result = output.result();
        assertThat(result.contradictionsFound()).hasSize(1);
        assertThat(result.contradictionsFound().getFirst().description()).contains("Reverse DCF growth hurdle");
        assertThat(result.isConsistent()).isFalse();
        assertThat(result.recommendedReruns()).hasSize(1);
        assertThat(result.recommendedReruns().getFirst().specialist()).isEqualTo(AgentType.VALUATION);
        assertThat(result.counterThesis()).contains("Adversarial counter-thesis");
    }
}
