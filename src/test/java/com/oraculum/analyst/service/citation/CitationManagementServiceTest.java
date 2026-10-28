package com.oraculum.analyst.service.citation;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentWorkflowState;
import com.oraculum.analyst.api.dto.AnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CitationManagementServiceTest {

    private CitationManagementService service;

    @BeforeEach
    void setUp() {
        service = new CitationManagementService(new JsonMapper());
    }

    @Test
    void testExtractReferencedCitations_onlyPicksReferenced() {
        Map<Integer, Object> allCitations = Map.of(
                1, Map.of("data", "item1"),
                2, Map.of("data", "item2"),
                3, Map.of("data", "item3")
        );

        String traceJson = "{\"summary\": \"Claim with citation [1] and another [3 ?]\"}";
        Map<Integer, Object> result = service.extractReferencedCitations(traceJson, allCitations);

        assertThat(result).containsOnlyKeys(1, 3);
        assertThat(result).doesNotContainKey(2);
    }

    @Test
    void testInjectUsedCitationsToTrace() {
        AgentWorkflowState state = new AgentWorkflowState();
        state.getCitationRegistry().getOrAssignCitationId(String.class, "k1", "payload1");
        state.getCitationRegistry().getOrAssignCitationId(String.class, "k2", "payload2");

        state.putAgentTrace("SPECIALIST", "Found revenue [1]");

        AgentContext ctx = AgentContext.builder()
                .state(state)
                .build();

        service.injectUsedCitationsToTrace(ctx);

        Map<Integer, Object> citationsInTrace = (Map<Integer, Object>) state.getAgentTrace().get(AgentWorkflowState.TRACE_CITATIONS_KEY);
        assertThat(citationsInTrace).containsOnlyKeys(1);
    }

    @Test
    void testFormatReportWithFootnote_addsDisclaimerWhenUnverifiedPresent() {
        AnalysisResult result = AnalysisResult.builder()
                .executiveSummary("Strong growth [1 ?] demonstrated.")
                .build();

        AnalysisResult formatted = service.formatReportWithFootnote(result);
        assertThat(formatted.executiveSummary()).contains("*Note: Citations marked with [?]");
    }

    @Test
    void testFormatReportWithFootnote_noDisclaimerWhenClean() {
        AnalysisResult result = AnalysisResult.builder()
                .executiveSummary("Strong growth [1] demonstrated.")
                .build();

        AnalysisResult formatted = service.formatReportWithFootnote(result);
        assertThat(formatted.executiveSummary()).doesNotContain("*Note: Citations marked with [?]");
    }
}
