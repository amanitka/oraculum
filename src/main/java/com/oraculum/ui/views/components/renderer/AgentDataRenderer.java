package com.oraculum.ui.views.components.renderer;

import com.oraculum.ui.views.components.renderer.agent.AgentCustomFieldRenderer;
import com.oraculum.ui.views.components.renderer.agent.AgentMetricsAndEvidenceRenderer;
import com.oraculum.ui.views.components.renderer.agent.AgentNarrativeRenderer;
import com.oraculum.ui.views.components.renderer.agent.AgentTileBarRenderer;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AgentDataRenderer {

    private static final Set<String> HANDLED_SCHEMA_KEYS = Set.of(
            "headline", "score", "confidence", "confidence_reasons", "recommendation",
            "outlook", "conviction", "management_sentiment", "sentiment", "managementSentiment",
            "strengths", "key_drivers", "weaknesses", "key_risks", "evidence",
            "business_risks", "financial_risks", "macro_risks", "sentiment_signals",
            "metrics", "factor_scores", "summary", "executive_summary", "recommendation_reasoning",
            "thesis_breakers", "scenarios", "what_would_change_this", "thesis",
            "top_bull_points", "top_bear_points", "valuation_one_liner", "valuation_dimensions"
    );

    private final AgentTileBarRenderer tileBarRenderer;
    private final AgentNarrativeRenderer narrativeRenderer;
    private final AgentMetricsAndEvidenceRenderer metricsAndEvidenceRenderer;
    private final AgentCustomFieldRenderer customFieldRenderer;

    public Component createAgentTabContent(String agentKey, JsonNode agentData, String jsonData) {
        if (agentData == null || agentData.isEmpty()) {
            return null;
        }
        VerticalLayout layout = createRootLayout();

        narrativeRenderer.addHeadlineIfPresent(layout, agentData);
        tileBarRenderer.addTilesRowIfPresent(layout, agentKey, agentData, jsonData);
        narrativeRenderer.addStrengthsIfPresent(layout, agentData, jsonData);
        narrativeRenderer.addWeaknessesIfPresent(layout, agentData, jsonData);
        narrativeRenderer.addRiskBucketsIfPresent(layout, agentData, jsonData);
        narrativeRenderer.addScenariosIfPresent(layout, agentData, jsonData);
        narrativeRenderer.addThesisBreakersIfPresent(layout, agentData, jsonData);
        metricsAndEvidenceRenderer.addEvidenceIfPresent(layout, agentData, jsonData);
        metricsAndEvidenceRenderer.addMetricsIfPresent(layout, agentData);
        metricsAndEvidenceRenderer.addValuationDimensionsIfPresent(layout, agentData);
        narrativeRenderer.addSummaryIfPresent(layout, agentData, jsonData);
        customFieldRenderer.addCustomFieldsIfPresent(layout, agentData, HANDLED_SCHEMA_KEYS, jsonData);

        return wrapInScroller(layout);
    }

    private VerticalLayout createRootLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.getStyle().set("max-width", "100%").set("box-sizing", "border-box").set("margin", "0 auto");
        return layout;
    }

    private Scroller wrapInScroller(Component content) {
        Scroller scroller = new Scroller(content, Scroller.ScrollDirection.VERTICAL);
        scroller.setWidthFull();
        scroller.getStyle().set("overflow-x", "hidden");
        return scroller;
    }
}
