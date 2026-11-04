package com.oraculum.ui.views.components.renderer.agent;

import com.oraculum.ui.views.components.renderer.MarkdownRenderer;
import com.oraculum.ui.views.components.renderer.RendererUtil;
import com.oraculum.ui.views.components.renderer.SnapshotUiSupport;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AgentTileBarRenderer {

    private final MarkdownRenderer markdownRenderer;

    public void addTilesRowIfPresent(VerticalLayout layout, String agentKey, JsonNode agentData, String jsonData) {
        HorizontalLayout tilesRow = buildTilesRow(agentKey, agentData, jsonData);
        if (tilesRow != null) {
            layout.add(tilesRow);
        }
    }

    public HorizontalLayout buildTilesRow(String agentKey, JsonNode agentData, String jsonData) {
        HorizontalLayout tilesRow = new HorizontalLayout();
        tilesRow.setWidthFull();
        tilesRow.getStyle().set("gap", "16px").set("margin-bottom", "20px").set("flex-wrap", "wrap");

        boolean hasPrimary = addPrimaryTiles(tilesRow, agentKey, agentData, jsonData);
        boolean hasVerdict = addVerdictTiles(tilesRow, agentData);
        boolean hasSentiment = addSentimentTile(tilesRow, agentData);

        return (hasPrimary || hasVerdict || hasSentiment) ? tilesRow : null;
    }

    private boolean addPrimaryTiles(HorizontalLayout tilesRow, String agentKey, JsonNode agentData, String jsonData) {
        boolean added = false;
        if (agentData.has("score")) {
            tilesRow.add(buildAgentScoreTile(agentKey, agentData));
            added = true;
        }
        if (agentData.has("confidence")) {
            Component content = renderConfidenceContent(
                    agentData.get("confidence"), agentData.path("confidence_reasons"), jsonData
            );
            tilesRow.add(createTile("Confidence", content));
            added = true;
        }
        return added;
    }

    private boolean addVerdictTiles(HorizontalLayout tilesRow, JsonNode agentData) {
        boolean added = false;
        if (agentData.has("recommendation")) {
            tilesRow.add(createTile("Recommendation", renderSentimentBadge(agentData.get("recommendation").asString())));
            added = true;
        }
        if (agentData.has("outlook")) {
            tilesRow.add(createTile("Outlook", renderSentimentBadge(agentData.get("outlook").asString())));
            added = true;
        }
        Integer conviction = extractConviction(agentData);
        if (conviction != null) {
            tilesRow.add(createTile("Conviction", renderConvictionMeter(conviction)));
            added = true;
        }
        return added;
    }

    private Integer extractConviction(JsonNode agentData) {
        if (agentData.has("conviction")) {
            return agentData.get("conviction").asInt(0);
        }
        if (agentData.path("metrics").has("bullish_conviction")) {
            return agentData.path("metrics").get("bullish_conviction").asInt(0);
        }
        return null;
    }

    private boolean addSentimentTile(HorizontalLayout tilesRow, JsonNode agentData) {
        String sentiment = extractSentiment(agentData);
        if (sentiment == null) {
            return false;
        }
        tilesRow.add(createTile("Sentiment", renderSentimentBadge(sentiment)));
        return true;
    }

    private String extractSentiment(JsonNode agentData) {
        if (agentData.has("management_sentiment")) {
            return agentData.get("management_sentiment").asString();
        }
        if (agentData.path("metrics").has("management_sentiment")) {
            return agentData.path("metrics").get("management_sentiment").asString();
        }
        if (agentData.has("sentiment")) {
            return agentData.get("sentiment").asString();
        }
        return null;
    }

    private Div buildAgentScoreTile(String agentKey, JsonNode agentData) {
        double score = agentData.get("score").asDouble(0.0);
        if (score > 10.0) {
            score = score / 10.0;
        }
        String badgeText = resolveScoreBadge(agentKey, agentData, score);
        String badgeTheme = SnapshotUiSupport.resolveDimensionTheme(badgeText);
        Div tile = SnapshotUiSupport.createScoreCard("Score", score, badgeText, badgeTheme);
        tile.getStyle().set("flex", "1 1 180px");
        return tile;
    }

    private String resolveScoreBadge(String agentKey, JsonNode agentData, double score) {
        if (agentData.has("valuation_zone") && !agentData.get("valuation_zone").asString().isBlank()) {
            return agentData.get("valuation_zone").asString();
        }
        if (agentData.has("rating") && !agentData.get("rating").asString().isBlank()) {
            return agentData.get("rating").asString();
        }
        return SnapshotUiSupport.resolveScoreBadgeLabel(agentKey, score);
    }

    private Component renderConfidenceContent(JsonNode confidence, JsonNode reasons, String jsonData) {
        VerticalLayout main = new VerticalLayout();
        main.setPadding(false);
        main.setSpacing(false);
        main.getStyle().set("gap", "12px");

        if (confidence.isObject()) {
            main.add(buildConfidenceBadgesRow(confidence));
        } else {
            main.add(createConfidenceBadge(null, confidence.asDouble(0.0)));
        }
        if (reasons != null && reasons.isArray() && !reasons.isEmpty()) {
            main.add(renderConfidenceReasonsList(reasons, jsonData));
        }
        return main;
    }

    private HorizontalLayout buildConfidenceBadgesRow(JsonNode confidence) {
        HorizontalLayout confGrid = new HorizontalLayout();
        confGrid.setSpacing(true);
        confGrid.add(createConfidenceBadge("Data: ", confidence.path("data").asDouble(0.0)));
        confGrid.add(createConfidenceBadge("Interpretation: ", confidence.path("interpretation").asDouble(0.0)));
        confGrid.add(createConfidenceBadge("Overall: ", confidence.path("overall").asDouble(0.0)));
        return confGrid;
    }

    private UnorderedList renderConfidenceReasonsList(JsonNode reasons, String jsonData) {
        UnorderedList reasonsList = new UnorderedList();
        reasonsList.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.85rem")
                .set("margin", "0")
                .set("padding-left", "16px");
        for (JsonNode reason : reasons) {
            ListItem li = new ListItem();
            li.add(markdownRenderer.renderMarkdownWithCitations(reason.asString(), jsonData));
            reasonsList.add(li);
        }
        return reasonsList;
    }

    private Component createConfidenceBadge(String label, double confVal) {
        double normalized = confVal > 1.0 ? confVal / 100.0 : confVal;
        String badgeText = (confVal > 1.0 ? (int) confVal : (int) (confVal * 100)) + "%";

        Span badge = new Span();
        if (label != null) {
            Span labelSpan = new Span(label);
            labelSpan.getStyle().set("margin-right", "4px").set("font-weight", "normal");
            badge.add(labelSpan);
        }
        badge.add(new Span(badgeText));
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add(normalized >= 0.8 ? "success" : (normalized >= 0.5 ? "contrast" : "error"));
        return badge;
    }

    private Component renderSentimentBadge(String sentimentStr) {
        if (sentimentStr == null || sentimentStr.isBlank()) {
            return new Span("N/A");
        }
        Span badge = new Span(RendererUtil.formatKeyTitle(sentimentStr));
        badge.getElement().getThemeList().add("badge");

        String upperStr = sentimentStr.toUpperCase(Locale.ROOT);
        if (upperStr.contains("BULLISH") || upperStr.contains("BUY")) {
            badge.getElement().getThemeList().add("success");
        } else if (upperStr.contains("BEARISH") || upperStr.contains("SELL")) {
            badge.getElement().getThemeList().add("error");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        badge.getStyle().set("font-size", "0.85rem").set("padding", "4px 10px");
        return badge;
    }

    private Component renderConvictionMeter(int conviction) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.getStyle().set("gap", "8px");

        HorizontalLayout meter = new HorizontalLayout();
        meter.setSpacing(false);
        meter.getStyle().set("gap", "3px").set("align-items", "center");
        for (int i = 1; i <= 5; i++) {
            meter.add(createConvictionBar(i <= conviction));
        }

        Span text = new Span(conviction + "/5");
        text.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);
        text.getStyle().set("color", "var(--lumo-body-text-color)");
        layout.add(meter, text);
        return layout;
    }

    private Span createConvictionBar(boolean active) {
        Span bar = new Span();
        bar.getStyle()
                .set("width", "10px")
                .set("height", "14px")
                .set("border-radius", "2px")
                .set("background", active ? "var(--lumo-primary-color)" : "rgba(255, 255, 255, 0.15)");
        return bar;
    }

    private Component createTile(String titleText, Component content) {
        Div tile = new Div();
        tile.getStyle()
                .set("flex", "1 1 180px")
                .set("background", "rgba(var(--lumo-contrast-rgb), 0.03)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "8px")
                .set("padding", "12px 16px");

        H4 title = new H4(titleText);
        title.getStyle()
                .set("margin", "0 0 8px 0")
                .set("font-size", "0.8rem")
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)");

        tile.add(title, content);
        return tile;
    }
}
