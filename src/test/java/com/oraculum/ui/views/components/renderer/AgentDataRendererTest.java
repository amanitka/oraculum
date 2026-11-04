package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.ui.views.components.renderer.agent.AgentCustomFieldRenderer;
import com.oraculum.ui.views.components.renderer.agent.AgentMetricsAndEvidenceRenderer;
import com.oraculum.ui.views.components.renderer.agent.AgentNarrativeRenderer;
import com.oraculum.ui.views.components.renderer.agent.AgentTileBarRenderer;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AgentDataRendererTest {

    private JsonMapper jsonMapper;
    private AgentDataRenderer renderer;

    @BeforeEach
    void setUp() {
        jsonMapper = new JsonMapper();
        MarkdownRenderer markdownRenderer = new MarkdownRenderer(jsonMapper);
        renderer = new AgentDataRenderer(
                new AgentTileBarRenderer(markdownRenderer),
                new AgentNarrativeRenderer(markdownRenderer),
                new AgentMetricsAndEvidenceRenderer(markdownRenderer),
                new AgentCustomFieldRenderer(markdownRenderer)
        );
    }

    @Test
    void createAgentTabContent_whenNullOrEmpty_returnsNull() {
        assertThat(renderer.createAgentTabContent("VALUATION", null, "{}")).isNull();
        assertThat(renderer.createAgentTabContent("VALUATION", jsonMapper.readTree("{}"), "{}")).isNull();
    }

    @Test
    void createAgentTabContent_forRiskAndValuationSpecialist_rendersTilesListsEvidenceAndMetrics() {
        String json = """
                {
                  "headline": "Manageable operational risks with elevated valuation tension",
                  "score": 7.6,
                  "confidence": { "data": 0.95, "interpretation": 0.85, "overall": 0.90 },
                  "confidence_reasons": ["Data completeness: 100%", "Interpretation confidence: 85%"],
                  "sentiment": "NEUTRAL",
                  "strengths": ["Strong balance sheet"],
                  "business_risks": ["Customer concentration"],
                  "sentiment_signals": ["Steady institutional ownership"],
                  "evidence": [
                    {
                      "metric": "Net Debt / EBITDA",
                      "value": "0.2x",
                      "previous_value": "0.4x",
                      "trend": "IMPROVING",
                      "source_type": "REPORTED",
                      "significance": "Minimal leverage risk"
                    }
                  ],
                  "metrics": {
                    "reverse_dcf_zone": "FAIR",
                    "forward_eps": "4.50",
                    "tension_note": "WARNING: Two-stage DCF resolves single-stage hurdle tension"
                  },
                  "valuation_dimensions": {
                    "eps_reconciliation_status": "ALIGNED"
                  },
                  "summary": "Overall risk profile remains **well-controlled**."
                }
                """;
        JsonNode node = jsonMapper.readTree(json);

        Component result = renderer.createAgentTabContent(AgentType.RISK.name(), node, json);

        assertThat(result).isInstanceOf(Scroller.class);
        Scroller scroller = (Scroller) result;
        assertThat(scroller.getContent()).isInstanceOf(VerticalLayout.class);
        VerticalLayout content = (VerticalLayout) scroller.getContent();
        assertThat(content.getComponentCount()).isGreaterThanOrEqualTo(8);
        assertThat(collectText(content))
                .contains("Manageable operational risks")
                .contains("Low Risk")
                .contains("Business Risks")
                .contains("Sentiment Signals")
                .contains("Evidence & Data")
                .contains("Key Metrics")
                .contains("Valuation Dimensions");
    }

    @Test
    void createAgentTabContent_forSynthesizer_rendersFactorScoresScenariosAndThesisBreakers() {
        String json = """
                {
                  "headline": "High-quality compounder at reasonable two-stage hurdle",
                  "outlook": "BULLISH",
                  "conviction": 4,
                  "factor_scores": {
                    "fundamental_health": 8.8,
                    "valuation": 7.2,
                    "risk_profile": 7.6
                  },
                  "scenarios": {
                    "bull": { "description": "AI capex accelerates", "key_assumption": "45% FCF margin", "implied_upside": "+28%" },
                    "base": { "description": "Consensus FY28 met", "key_assumption": "42% FCF margin", "implied_move": "+12%" },
                    "bear": { "description": "Digestion cycle", "key_assumption": "35% FCF margin", "implied_downside": "-18%" }
                  },
                  "thesis_breakers": ["FY28 revenue growth drops below 10%"],
                  "executive_summary": "Balanced risk-reward."
                }
                """;
        JsonNode node = jsonMapper.readTree(json);

        Component result = renderer.createAgentTabContent(AgentType.SYNTHESIZER.name(), node, json);

        assertThat(result).isInstanceOf(Scroller.class);
        String renderedText = collectText(((Scroller) result).getContent());
        assertThat(renderedText)
                .contains("4/5")
                .contains("Factor Scores")
                .contains("Investment Scenarios")
                .contains("Bull Case")
                .contains("Base Case")
                .contains("Bear Case")
                .contains("Thesis Breakers & Monitoring Triggers");
    }

    @Test
    void createAgentTabContent_forCritic_rendersConsistencyContradictionsAndReruns() {
        String json = """
                {
                  "is_consistent": false,
                  "contradictions": [
                    {
                      "resolution": "UNRESOLVED",
                      "correction_type": "FACT_MISMATCH",
                      "agents_involved": ["FUNDAMENTALS", "CASH_FLOW"],
                      "description": "Margin expansion signal mismatch"
                    }
                  ],
                  "recommended_reruns": [
                    {
                      "specialist": "CASH_FLOW",
                      "severity": "HIGH",
                      "instruction": "Verify TTM FCF margin citation"
                    }
                  ]
                }
                """;
        JsonNode node = jsonMapper.readTree(json);

        Component result = renderer.createAgentTabContent(AgentType.CRITIC.name(), node, json);

        assertThat(result).isInstanceOf(Scroller.class);
        String renderedText = collectText(((Scroller) result).getContent());
        assertThat(renderedText)
                .contains("Consistency Status:")
                .contains("CONTRADICTION DETECTED")
                .contains("UNRESOLVED")
                .contains("CASH_FLOW (Severity HIGH)");
    }

    private String collectText(Component root) {
        StringBuilder sb = new StringBuilder();
        appendTextRecursively(root, sb);
        return sb.toString();
    }

    private void appendTextRecursively(Component comp, StringBuilder sb) {
        if (comp == null) {
            return;
        }
        String text = comp.getElement().getText();
        if (text != null && !text.isBlank()) {
            sb.append(text).append(' ');
        }
        comp.getChildren().forEach(child -> appendTextRecursively(child, sb));
    }
}
