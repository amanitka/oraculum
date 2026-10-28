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
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CriticAgentTest {

    private List<Contradiction> stubbedValidationChecks;
    private CriticAgentOutput stubbedLlmOutput;
    private CriticAgent agent;
    private AgentContext context;

    @BeforeEach
    void setUp() {
        stubbedValidationChecks = new ArrayList<>();
        stubbedLlmOutput = new CriticAgentOutput(List.of(), true, List.of(), "Default counter thesis");

        CompanyDto company = new CompanyDto(
                1, "NVDA", "US", "NVIDIA Corp", null, null, null, null, null, null, null, null, null, null
        );
        AgentWorkflowState state = new AgentWorkflowState();
        context = AgentContext.builder()
                .correlationId(UUID.randomUUID())
                .company(company)
                .analysisDate(LocalDate.now())
                .state(state)
                .build();

        PromptRegistry promptRegistry = new PromptRegistry() {
            @Override
            public String getPrompt(PromptType type) {
                return "Prompt for {{ ticker }} checks: {{ deterministic_checks }}";
            }
        };

        PreCriticValidationService preCriticValidationService = new PreCriticValidationService() {
            @Override
            public List<Contradiction> validate(AgentContext ctx) {
                return stubbedValidationChecks;
            }
        };

        LlmRouterApi llmRouterApi = new LlmRouterApi() {
            @Override
            public <T> LlmResponse<T> executeCall(LlmCallRequest<T> request) {
                return (LlmResponse<T>) new LlmResponse<>(
                        stubbedLlmOutput, new LlmMetrics(null, null, 150, 50, 200, 120L)
                );
            }
        };

        agent = new CriticAgent(llmRouterApi, promptRegistry, new JsonMapper(), preCriticValidationService);
    }

    @Test
    void getName_returnsCritic() {
        assertThat(agent.getName()).isEqualTo(AgentType.CRITIC);
    }

    @Test
    void run_mergesDeterministicChecksAndAppendsReruns() {
        stubbedValidationChecks = List.of(new Contradiction(
                "Reverse DCF growth hurdle mismatch",
                List.of("VALUATION"),
                CorrectionType.REASONING_ERROR,
                Resolution.RERUN_APPLIED
        ));

        stubbedLlmOutput = new CriticAgentOutput(
                List.of(),
                true,
                List.of(),
                "Adversarial counter-thesis: Hyperscaler capex digestion poses sharp margin risk."
        );

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

    @Test
    void run_whenOnlyNarrativeReconciledContradictions_isConsistentRemainsTrue() {
        stubbedValidationChecks = List.of(new Contradiction(
                "Fiscal year label offset between SEC and SimFin",
                List.of("FUNDAMENTALS", "EARNINGS_ESTIMATES"),
                CorrectionType.TIME_WINDOW_MISMATCH,
                Resolution.NARRATIVE_RECONCILED
        ));

        stubbedLlmOutput = new CriticAgentOutput(
                List.of(),
                false,
                List.of(),
                "Counter-thesis text"
        );

        AgentOutput<CriticAgentOutput> output = agent.run(context);

        assertThat(output.result().contradictionsFound()).hasSize(1);
        assertThat(output.result().isConsistent()).isTrue();
        assertThat(output.result().recommendedReruns()).isEmpty();
    }
}
