package com.oraculum.ui.views.components.renderer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AgentDataRenderer {

    private static final Set<String> HANDLED_SCHEMA_KEYS = Set.of(
            "headline", "score", "confidence", "confidence_reasons", "recommendation",
            "outlook", "conviction", "management_sentiment", "sentiment", "managementSentiment",
            "strengths", "key_drivers", "weaknesses", "key_risks", "evidence",
            "business_risks", "financial_risks", "macro_risks", "sentiment_signals",
            "metrics", "factor_scores", "summary", "executive_summary", "recommendation_reasoning",
            "thesis_breakers", "scenarios", "what_would_change_this", "thesis", "top_bull_points", "top_bear_points", "valuation_one_liner", "valuation_dimensions"
    );

    private final MarkdownRenderer markdownRenderer;

    public Component createAgentTabContent(JsonNode agentData, String jsonData) {
        if (agentData == null || agentData.isEmpty()) {
            return null;
        }

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.getStyle().set("max-width", "100%").set("box-sizing", "border-box").set("margin", "0 auto");

        addHeadlineIfPresent(layout, agentData);
        addTilesRowIfPresent(layout, agentData, jsonData);
        addStrengthsIfPresent(layout, agentData, jsonData);
        addWeaknessesIfPresent(layout, agentData, jsonData);
        addRiskBucketsIfPresent(layout, agentData, jsonData);
        addScenariosIfPresent(layout, agentData, jsonData);
        addThesisBreakersIfPresent(layout, agentData, jsonData);
        addEvidenceIfPresent(layout, agentData, jsonData);
        addMetricsIfPresent(layout, agentData);
        addValuationDimensionsIfPresent(layout, agentData);
        addSummaryIfPresent(layout, agentData, jsonData);
        addCustomFieldsIfPresent(layout, agentData, jsonData);

        return wrapInScroller(layout);
    }

    private void addHeadlineIfPresent(VerticalLayout layout, JsonNode agentData) {
        if (agentData.has("headline")) {
            layout.add(renderHeadline(agentData.get("headline")));
        }
    }

    private void addTilesRowIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        HorizontalLayout tilesRow = buildTilesRow(agentData, jsonData);
        if (tilesRow != null) {
            layout.add(tilesRow);
        }
    }

    private HorizontalLayout buildTilesRow(JsonNode agentData, String jsonData) {
        HorizontalLayout tilesRow = new HorizontalLayout();
        tilesRow.setWidthFull();
        tilesRow.getStyle().set("gap", "16px").set("margin-bottom", "20px").set("flex-wrap", "wrap");
        boolean hasTiles = false;

        if (agentData.has("score")) {
            tilesRow.add(buildAgentScoreTile(agentData));
            hasTiles = true;
        }
        if (agentData.has("confidence")) {
            tilesRow.add(createTile("Confidence", renderConfidenceContent(agentData.get("confidence"), agentData.path("confidence_reasons"), jsonData)));
            hasTiles = true;
        }
        if (agentData.has("recommendation")) {
            tilesRow.add(createTile("Recommendation", renderSentimentBadge(agentData.get("recommendation").asString())));
            hasTiles = true;
        }
        if (agentData.has("outlook")) {
            tilesRow.add(createTile("Outlook", renderSentimentBadge(agentData.get("outlook").asString())));
            hasTiles = true;
        }
        if (agentData.has("conviction")) {
            tilesRow.add(createTile("Conviction", renderConvictionMeter(agentData.get("conviction").asInt(0))));
            hasTiles = true;
        } else if (agentData.path("metrics").has("bullish_conviction")) {
            tilesRow.add(createTile("Conviction", renderConvictionMeter(agentData.path("metrics").get("bullish_conviction").asInt(0))));
            hasTiles = true;
        }
        if (agentData.has("management_sentiment")) {
            tilesRow.add(createTile("Sentiment", renderSentimentBadge(agentData.get("management_sentiment").asString())));
            hasTiles = true;
        } else if (agentData.path("metrics").has("management_sentiment")) {
            tilesRow.add(createTile("Sentiment", renderSentimentBadge(agentData.path("metrics").get("management_sentiment").asString())));
            hasTiles = true;
        } else if (agentData.has("sentiment")) {
            tilesRow.add(createTile("Sentiment", renderSentimentBadge(agentData.get("sentiment").asString())));
            hasTiles = true;
        }

        return hasTiles ? tilesRow : null;
    }

    private void addStrengthsIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        if (agentData.has("strengths")) {
            layout.add(renderStrengths(agentData.get("strengths"), "Key Strengths", jsonData));
        } else if (agentData.has("key_drivers")) {
            layout.add(renderStrengths(agentData.get("key_drivers"), "Key Drivers", jsonData));
        }
    }

    private void addWeaknessesIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        if (agentData.has("weaknesses")) {
            layout.add(renderWeaknesses(agentData.get("weaknesses"), "Key Weaknesses", jsonData));
        } else if (agentData.has("key_risks")) {
            layout.add(renderWeaknesses(agentData.get("key_risks"), "Key Risks", jsonData));
        }
    }

    private void addRiskBucketsIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        if (agentData.has("business_risks")) {
            layout.add(renderWeaknesses(agentData.get("business_risks"), "Business Risks", jsonData));
        }
        if (agentData.has("financial_risks")) {
            layout.add(renderWeaknesses(agentData.get("financial_risks"), "Financial Risks", jsonData));
        }
        if (agentData.has("macro_risks")) {
            layout.add(renderWeaknesses(agentData.get("macro_risks"), "Macro Risks", jsonData));
        }
        if (agentData.has("sentiment_signals")) {
            layout.add(renderInfoList(agentData.get("sentiment_signals"), "Sentiment Signals", jsonData));
        }
    }

    private void addValuationDimensionsIfPresent(VerticalLayout layout, JsonNode agentData) {
        if (agentData.has("valuation_dimensions")) {
            layout.add(renderMetrics(agentData.get("valuation_dimensions"), "Valuation Dimensions"));
        }
    }

    private void addEvidenceIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        if (agentData.has("evidence")) {
            layout.add(renderEvidence(agentData.get("evidence"), jsonData));
        }
    }

    private void addMetricsIfPresent(VerticalLayout layout, JsonNode agentData) {
        if (agentData.has("factor_scores")) {
            layout.add(renderFactorScoresSection(agentData.get("factor_scores")));
        } else if (agentData.has("metrics")) {
            layout.add(renderMetrics(agentData.get("metrics"), "Key Metrics"));
        }
    }

    private void addSummaryIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        if (agentData.has("summary")) {
            layout.add(renderSummary(agentData.get("summary"), jsonData, "Summary"));
        } else if (agentData.has("executive_summary")) {
            layout.add(renderSummary(agentData.get("executive_summary"), jsonData, "Executive Summary"));
        }
    }

    private void addCustomFieldsIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        for (Map.Entry<String, JsonNode> field : agentData.properties()) {
            String key = field.getKey();
            if (HANDLED_SCHEMA_KEYS.contains(key)) {
                continue;
            }
            renderCustomField(layout, key, field.getValue(), jsonData);
        }
    }

    private void renderCustomField(VerticalLayout layout, String key, JsonNode value, String jsonData) {
        if ("is_consistent".equals(key) || "isConsistent".equals(key)) {
            boolean isConsistent = value.asBoolean(true);
            HorizontalLayout consistencyRow = new HorizontalLayout();
            consistencyRow.setAlignItems(FlexComponent.Alignment.CENTER);
            consistencyRow.getStyle().set("margin-top", "20px").set("margin-bottom", "12px").set("gap", "12px");

            H4 label = new H4("Consistency Status:");
            label.getStyle()
                    .set("margin", "0")
                    .set("font-size", "0.9rem")
                    .set("font-weight", "700")
                    .set("letter-spacing", "0.5px")
                    .set("color", "var(--lumo-secondary-text-color)");

            Span statusBadge = new Span(isConsistent ? "CONSISTENT" : "CONTRADICTION DETECTED");
            statusBadge.getElement().getThemeList().add("badge");
            statusBadge.getElement().getThemeList().add(isConsistent ? "success" : "error");
            statusBadge.getStyle().set("font-size", "0.85rem").set("padding", "4px 10px");

            consistencyRow.add(label, statusBadge);
            layout.add(consistencyRow);
            return;
        }

        H3 fieldHeader = new H3(RendererUtil.formatKeyTitle(key));
        fieldHeader.getStyle()
                .set("margin-top", "2rem")
                .set("margin-bottom", "1rem")
                .set("font-size", "1.1rem")
                .set("font-weight", "600")
                .set("color", "var(--lumo-header-text-color)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "0.5rem");
        layout.add(fieldHeader);

        if (value.isArray()) {
            layout.add(renderArrayField(key, value, jsonData));
        } else if (value.isObject()) {
            layout.add(renderObjectField(value));
        } else {
            layout.add(renderPrimitiveField(value, jsonData));
        }
    }

    private Component renderSentimentBadge(String sentimentStr) {
        if (sentimentStr == null || sentimentStr.isBlank()) {
            return new Span("N/A");
        }
        String labelText = RendererUtil.formatKeyTitle(sentimentStr);
        Span badge = new Span(labelText);
        badge.getElement().getThemeList().add("badge");

        String upperStr = sentimentStr.toUpperCase();
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
            Span bar = new Span();
            bar.getStyle()
                    .set("width", "10px")
                    .set("height", "14px")
                    .set("border-radius", "2px");
            if (i <= conviction) {
                bar.getStyle().set("background", "var(--lumo-primary-color)");
            } else {
                bar.getStyle().set("background", "rgba(255, 255, 255, 0.15)");
            }
            meter.add(bar);
        }

        Span text = new Span(conviction + "/5");
        text.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);
        text.getStyle().set("color", "var(--lumo-body-text-color)");

        layout.add(meter, text);
        return layout;
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

    private Component renderArrayField(String key, JsonNode value, String jsonData) {
        if (value == null || value.isEmpty()) {
            Span emptySpan = new Span("None recorded");
            emptySpan.getStyle().set("color", "var(--lumo-tertiary-text-color)").set("font-style", "italic").set("font-size", "0.9rem");
            return emptySpan;
        }
        UnorderedList list = new UnorderedList();
        list.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.FontSize.SMALL);
        list.getStyle().set("list-style-type", "none").set("padding-left", "0");
        for (JsonNode item : value) {
            if (item.isObject()) {
                if ("recommended_reruns".equals(key)) {
                    list.add(new ListItem(renderRecommendedRerunItem(item)));
                } else if ("contradictions_found".equals(key) || "contradictions".equals(key)) {
                    list.add(new ListItem(renderContradictionItem(item)));
                } else {
                    list.add(new ListItem(renderObjectField(item)));
                }
            } else {
                ListItem li = new ListItem();
                li.add(markdownRenderer.renderMarkdownWithCitations(item.asString(), jsonData));
                list.add(li);
            }
        }
        return list;
    }

    private Component renderRecommendedRerunItem(JsonNode item) {
        Div rerunDiv = new Div();
        rerunDiv.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.Padding.SMALL,
                LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.Bottom.SMALL);

        String specialist = item.path("specialist").asString("");
        String severity = item.path("severity").asString("");
        String instruction = item.path("instruction").asString("");

        Span badge = new Span(specialist + " (Severity " + severity + ")");
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add("error");
        badge.addClassNames(LumoUtility.Margin.Bottom.SMALL);
        badge.getStyle().set("display", "inline-block");

        Paragraph inst = new Paragraph(instruction);
        inst.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.SMALL);

        rerunDiv.add(badge, inst);
        return rerunDiv;
    }

    private Component renderContradictionItem(JsonNode item) {
        Div contradictionDiv = new Div();
        contradictionDiv.getStyle()
                .set("background", "rgba(var(--lumo-contrast-rgb), 0.03)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-left", "4px solid var(--lumo-warning-color)")
                .set("border-radius", "0 8px 8px 0")
                .set("padding", "16px 20px")
                .set("margin-bottom", "16px");

        HorizontalLayout badgeRow = new HorizontalLayout();
        badgeRow.setAlignItems(FlexComponent.Alignment.CENTER);
        badgeRow.getStyle().set("gap", "10px").set("flex-wrap", "wrap").set("margin-bottom", "12px");

        String resolution = item.path("resolution").asString("");
        if (!resolution.isBlank()) {
            Span resBadge = new Span(resolution.replace("_", " "));
            resBadge.getElement().getThemeList().add("badge");
            if ("UNRESOLVED".equalsIgnoreCase(resolution)) {
                resBadge.getElement().getThemeList().add("error");
            } else if ("RERUN_APPLIED".equalsIgnoreCase(resolution)) {
                resBadge.getElement().getThemeList().add("contrast");
            } else {
                resBadge.getElement().getThemeList().add("success");
            }
            resBadge.getStyle().set("font-weight", "600").set("padding", "4px 10px");
            badgeRow.add(resBadge);
        }

        String correctionType = item.path("correction_type").asString("");
        if (!correctionType.isBlank()) {
            Span typeBadge = new Span(correctionType.replace("_", " "));
            typeBadge.getElement().getThemeList().add("badge");
            typeBadge.getElement().getThemeList().add("primary");
            typeBadge.getStyle().set("font-weight", "600").set("padding", "4px 10px");
            badgeRow.add(typeBadge);
        }

        JsonNode agentsNode = item.path("agents_involved");
        if (agentsNode.isArray() && !agentsNode.isEmpty()) {
            Span agentsLabel = new Span("Agents:");
            agentsLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY, LumoUtility.FontWeight.BOLD);
            agentsLabel.getStyle().set("margin-left", "6px");
            badgeRow.add(agentsLabel);

            for (JsonNode agentNode : agentsNode) {
                Span agentBadge = new Span(agentNode.asString());
                agentBadge.getElement().getThemeList().add("badge");
                agentBadge.getStyle().set("font-size", "0.8rem");
                badgeRow.add(agentBadge);
            }
        }

        String description = item.path("description").asString("");
        Paragraph descPara = new Paragraph(description);
        descPara.getStyle()
                .set("margin", "0")
                .set("font-size", "0.95rem")
                .set("line-height", "1.6")
                .set("color", "var(--lumo-body-text-color)");

        contradictionDiv.add(badgeRow, descPara);
        return contradictionDiv;
    }

    private Component renderObjectField(JsonNode value) {
        if (value == null || value.isEmpty()) {
            return new Span("None");
        }
        Div container = new Div();
        container.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.Padding.SMALL,
                LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.Bottom.SMALL);

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        for (Map.Entry<String, JsonNode> entry : value.properties()) {
            String key = RendererUtil.formatKeyTitle(entry.getKey());
            JsonNode val = entry.getValue();

            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.BASELINE);

            Span label = new Span(key + ": ");
            label.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);
            label.getStyle().set("min-width", "130px");

            Component valComp;
            if (val.isArray()) {
                valComp = renderArrayField(entry.getKey(), val, null);
            } else if (val.isObject()) {
                valComp = renderObjectField(val);
            } else {
                Span valSpan = new Span(val.asString());
                valSpan.addClassNames(LumoUtility.FontSize.SMALL);
                valComp = valSpan;
            }

            row.add(label, valComp);
            layout.add(row);
        }

        container.add(layout);
        return container;
    }

    private Component renderPrimitiveField(JsonNode value, String jsonData) {
        return markdownRenderer.renderMarkdownWithCitations(value.asString(), jsonData);
    }

    private Component renderHeadline(JsonNode value) {
        Div headline = new Div();
        headline.setText(value.asString(""));
        headline.getStyle()
                .set("background", "rgba(var(--lumo-primary-color-rgb), 0.05)")
                .set("border-left", "3px solid var(--lumo-primary-color)")
                .set("border-radius", "0 4px 4px 0")
                .set("padding", "12px 16px")
                .set("margin-top", "16px")
                .set("margin-bottom", "16px")
                .set("font-size", "1.1rem")
                .set("line-height", "1.6")
                .set("color", "var(--lumo-body-text-color)");
        return headline;
    }

    private Div buildAgentScoreTile(JsonNode agentData) {
        double score = agentData.get("score").asDouble(0.0);
        if (score > 10.0) {
            score = score / 10.0;
        }
        String badgeText = resolveScoreBadge(agentData, score);
        String badgeTheme = SnapshotUiSupport.resolveDimensionTheme(badgeText);
        Div tile = SnapshotUiSupport.createScoreCard("Score", score, badgeText, badgeTheme);
        tile.getStyle().set("flex", "1 1 180px");
        return tile;
    }

    private String resolveScoreBadge(JsonNode agentData, double score) {
        if (agentData.has("valuation_zone") && !agentData.get("valuation_zone").asString().isBlank()) {
            return agentData.get("valuation_zone").asString();
        }
        if (agentData.has("rating") && !agentData.get("rating").asString().isBlank()) {
            return agentData.get("rating").asString();
        }
        return SnapshotUiSupport.resolveScoreBadgeLabel(score);
    }

    private Component renderFactorScoresSection(JsonNode factorScores) {
        if (factorScores == null || factorScores.isEmpty()) {
            return new Span();
        }
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.setWidthFull();
        section.getStyle().set("margin-bottom", "20px");

        H4 header = new H4("Factor Scores");
        header.getStyle()
                .set("margin-top", "24px")
                .set("margin-bottom", "14px")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "8px")
                .set("font-size", "1.1rem")
                .set("color", "var(--lumo-header-text-color)");
        section.add(header);

        Div grid = new Div();
        grid.setWidthFull();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(200px, 1fr))")
                .set("gap", "12px");

        for (Map.Entry<String, JsonNode> entry : factorScores.properties()) {
            double score = entry.getValue().asDouble(0.0);
            if (score > 10.0) {
                score = score / 10.0;
            }
            String title = formatFactorScoreTitle(entry.getKey());
            String badgeText = SnapshotUiSupport.resolveScoreBadgeLabel(score);
            String badgeTheme = SnapshotUiSupport.resolveScoreBadgeTheme(score);
            grid.add(SnapshotUiSupport.createScoreCard(title, score, badgeText, badgeTheme));
        }
        section.add(grid);
        return section;
    }

    private String formatFactorScoreTitle(String key) {
        if (key == null || key.isBlank()) return "Score";
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "fundamental_health", "fundamental" -> "Fundamental Health";
            case "valuation" -> "Valuation Hurdle";
            case "growth_prospects", "growth" -> "Growth Prospects";
            case "risk_profile", "risk" -> "Risk & Market Setup";
            default -> SnapshotUiSupport.formatDimensionLabel(key);
        };
    }

    private Component renderConfidenceContent(JsonNode confidence, JsonNode reasons, String jsonData) {
        VerticalLayout main = new VerticalLayout();
        main.setPadding(false);
        main.setSpacing(false);
        main.getStyle().set("gap", "12px");

        if (confidence.isObject()) {
            HorizontalLayout confGrid = new HorizontalLayout();
            confGrid.setSpacing(true);
            confGrid.add(createConfidenceBadge("Data: ", confidence.path("data").asDouble(0.0)));
            confGrid.add(createConfidenceBadge("Interpretation: ", confidence.path("interpretation").asDouble(0.0)));
            confGrid.add(createConfidenceBadge("Overall: ", confidence.path("overall").asDouble(0.0)));
            main.add(confGrid);
        } else {
            main.add(createConfidenceBadge(null, confidence.asDouble(0.0)));
        }

        if (reasons != null && reasons.isArray() && !reasons.isEmpty()) {
            main.add(renderConfidenceReasonsList(reasons, jsonData));
        }
        return main;
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
        String badgeText;
        if (confVal > 1.0) {
            badgeText = (int) confVal + "%";
            confVal = confVal / 100.0;
        } else {
            badgeText = (int) (confVal * 100) + "%";
        }

        Span badge = new Span();
        if (label != null) {
            Span labelSpan = new Span(label);
            labelSpan.getStyle().set("margin-right", "4px").set("font-weight", "normal");
            badge.add(labelSpan);
        }
        badge.add(new Span(badgeText));

        badge.getElement().getThemeList().add("badge");
        if (confVal >= 0.8) badge.getElement().getThemeList().add("success");
        else if (confVal >= 0.5) badge.getElement().getThemeList().add("contrast");
        else badge.getElement().getThemeList().add("error");

        return badge;
    }

    private Component renderStrengths(JsonNode value, String title, String jsonData) {
        if (value == null || value.isEmpty()) return new Span();
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getStyle().set("margin-bottom", "20px");

        H4 header = new H4(title);
        header.getStyle()
                .set("margin-top", "24px")
                .set("margin-bottom", "16px")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "8px");
        layout.add(header);

        for (JsonNode item : value) {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.START);
            row.getStyle().set("margin-bottom", "16px");
            com.vaadin.flow.component.icon.Icon icon = VaadinIcon.CHECK_CIRCLE.create();
            icon.setColor("var(--lumo-success-color)");
            icon.setSize("16px");
            icon.getStyle().set("margin-top", "4px").set("flex-shrink", "0");
            Component textComp = markdownRenderer.renderMarkdownWithCitations(item.asString(), jsonData);
            textComp.getStyle().set("line-height", "1.6");
            row.add(icon, textComp);
            layout.add(row);
        }
        return layout;
    }

    private Component renderWeaknesses(JsonNode value, String title, String jsonData) {
        if (value == null || value.isEmpty()) return new Span();
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getStyle().set("margin-bottom", "20px");

        H4 header = new H4(title);
        header.getStyle()
                .set("margin-top", "24px")
                .set("margin-bottom", "16px")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "8px");
        layout.add(header);

        for (JsonNode item : value) {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.START);
            row.getStyle().set("margin-bottom", "16px");
            com.vaadin.flow.component.icon.Icon icon = VaadinIcon.WARNING.create();
            icon.setColor("var(--lumo-error-color)");
            icon.setSize("16px");
            icon.getStyle().set("margin-top", "4px").set("flex-shrink", "0");
            Component textComp = markdownRenderer.renderMarkdownWithCitations(item.asString(), jsonData);
            textComp.getStyle().set("line-height", "1.6");
            row.add(icon, textComp);
            layout.add(row);
        }
        return layout;
    }

    private Component renderInfoList(JsonNode value, String title, String jsonData) {
        if (value == null || value.isEmpty()) return new Span();
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getStyle().set("margin-bottom", "20px");

        H4 header = new H4(title);
        header.getStyle()
                .set("margin-top", "24px")
                .set("margin-bottom", "16px")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "8px");
        layout.add(header);

        for (JsonNode item : value) {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.START);
            row.getStyle().set("margin-bottom", "16px");
            Icon icon = VaadinIcon.INFO_CIRCLE.create();
            icon.setColor("var(--lumo-primary-color)");
            icon.setSize("16px");
            icon.getStyle().set("margin-top", "4px").set("flex-shrink", "0");
            Component textComp = markdownRenderer.renderMarkdownWithCitations(item.asString(), jsonData);
            textComp.getStyle().set("line-height", "1.6");
            row.add(icon, textComp);
            layout.add(row);
        }
        return layout;
    }

    private Component renderEvidence(JsonNode value, String jsonData) {
        if (value == null || !value.isArray() || value.isEmpty()) return new Span();
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getStyle().set("margin-bottom", "20px");

        H4 header = new H4("Evidence & Data");
        header.getStyle()
                .set("margin-top", "24px")
                .set("margin-bottom", "14px")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "8px")
                .set("font-size", "1.1rem")
                .set("color", "var(--lumo-header-text-color)");
        layout.add(header);

        Grid<JsonNode> grid = new Grid<>();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_WRAP_CELL_CONTENT);
        grid.addClassName("screener-grid");
        grid.setWidthFull();
        grid.getStyle().set("font-size", "0.88rem");

        grid.addColumn(item -> item.path("metric").asString(""))
                .setHeader("Metric")
                .setWidth("220px")
                .setFlexGrow(1);

        grid.addComponentColumn(item -> {
            Span currentSpan = new Span(item.path("value").asString(""));
            currentSpan.getStyle().set("font-weight", "bold").set("color", "var(--lumo-header-text-color)");
            return currentSpan;
        }).setHeader("Current").setWidth("130px").setFlexGrow(0);

        grid.addComponentColumn(item -> {
            String prev = item.path("previous_value").asString("");
            Span prevSpan = new Span(prev.isBlank() ? "-" : prev);
            prevSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
            return prevSpan;
        }).setHeader("Previous").setWidth("130px").setFlexGrow(0);

        grid.addComponentColumn(item -> renderTrendIcon(item.path("trend").asString("")))
                .setHeader("Trend")
                .setWidth("70px")
                .setFlexGrow(0);

        grid.addComponentColumn(item -> {
            String sourceType = item.path("source_type").asString("");
            if (sourceType.isBlank()) return new Span("-");
            String formatted = SnapshotUiSupport.formatDimensionLabel(sourceType);
            String theme = switch (sourceType.toUpperCase(Locale.ROOT)) {
                case "REPORTED" -> "primary";
                case "CALCULATED" -> "contrast";
                case "DERIVED" -> "warning";
                case "ESTIMATED" -> "success";
                default -> "contrast";
            };
            return SnapshotUiSupport.createStatusBadge(formatted, theme);
        }).setHeader("Type").setWidth("110px").setFlexGrow(0);

        grid.addComponentColumn(item -> {
            String sig = item.path("significance").asString("");
            if (sig.isBlank()) return new Span("-");
            Component sigComp = markdownRenderer.renderMarkdownWithCitations(sig, jsonData);
            sigComp.getStyle().set("font-size", "0.85rem").set("line-height", "1.45");
            return sigComp;
        }).setHeader("Why It Matters").setFlexGrow(4);

        List<JsonNode> itemList = new ArrayList<>();
        value.forEach(itemList::add);
        grid.setItems(itemList);
        grid.setAllRowsVisible(true);

        layout.add(grid);
        return layout;
    }

    private Component renderTrendIcon(String trend) {
        String upper = trend != null ? trend.toUpperCase() : "";
        Icon icon;
        String tooltipText;

        if (upper.contains("IMPROV") || upper.contains("UP") || upper.contains("BULL")) {
            icon = VaadinIcon.ARROW_UP.create();
            icon.setColor("var(--lumo-success-color)");
            icon.getStyle().set("color", "var(--lumo-success-color)");
            tooltipText = "Improving (Favorable implication for the thesis)";
        } else if (upper.contains("DETERIORAT") || upper.contains("DOWN") || upper.contains("DECLIN") || upper.contains("BEAR")) {
            icon = VaadinIcon.ARROW_DOWN.create();
            icon.setColor("var(--lumo-error-color)");
            icon.getStyle().set("color", "var(--lumo-error-color)");
            tooltipText = "Deteriorating (Adverse / higher risk implication)";
        } else {
            icon = VaadinIcon.ARROW_RIGHT.create();
            icon.setColor("#8c9197");
            icon.getStyle().set("color", "#8c9197");
            tooltipText = "Stable / Neutral";
        }

        icon.setSize("14px");
        icon.getElement().setAttribute("title", tooltipText);
        return icon;
    }

    private Component renderMetrics(JsonNode value, String title) {
        if (value == null || value.isEmpty()) return new Span();
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setWidthFull();
        layout.getStyle().set("margin-bottom", "20px");

        H4 header = new H4(title);
        header.getStyle()
                .set("margin-top", "24px")
                .set("margin-bottom", "14px")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "8px")
                .set("font-size", "1.1rem")
                .set("color", "var(--lumo-header-text-color)");
        layout.add(header);

        Div grid = new Div();
        grid.setWidthFull();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(180px, 1fr))")
                .set("grid-auto-flow", "dense")
                .set("gap", "12px");

        for (Map.Entry<String, JsonNode> entry : value.properties()) {
            grid.add(buildMetricCard(entry.getKey(), entry.getValue()));
        }
        layout.add(grid);
        return layout;
    }

    private Div buildMetricCard(String rawKey, JsonNode valNode) {
        Div card = SnapshotUiSupport.createNeutralCard();
        card.getStyle()
                .set("padding", "10px 14px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "space-between")
                .set("gap", "6px");

        String valStr = extractMetricString(valNode);
        if (valStr.length() > 65) {
            card.getStyle().set("grid-column", "span 2");
        }

        Span keySpan = new Span(SnapshotUiSupport.formatMetricLabel(rawKey));
        keySpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.74rem")
                .set("font-weight", "600")
                .set("letter-spacing", "0.02em")
                .set("line-height", "1.25");

        card.add(keySpan, buildMetricValueComponent(rawKey, valStr));
        return card;
    }

    private Component buildMetricValueComponent(String rawKey, String valStr) {
        if (isStatusOrEnum(rawKey, valStr)) {
            String theme = SnapshotUiSupport.resolveDimensionTheme(valStr);
            Span badge = SnapshotUiSupport.createStatusBadge(SnapshotUiSupport.formatDimensionLabel(valStr), theme);
            badge.getStyle().set("align-self", "flex-start");
            return badge;
        }

        int colonIdx = valStr.indexOf(':');
        if (colonIdx > 0 && colonIdx <= 25 && valStr.length() > 30) {
            String prefix = valStr.substring(0, colonIdx).trim();
            if (isSentimentPrefix(prefix)) {
                return buildPrefixedDescriptiveBlock(prefix, valStr.substring(colonIdx + 1).trim());
            }
        }

        if (valStr.length() > 25) {
            return buildDescriptiveText(valStr);
        }
        return buildStandardValueSpan(rawKey, valStr);
    }

    private Component buildPrefixedDescriptiveBlock(String prefix, String body) {
        VerticalLayout block = new VerticalLayout();
        block.setPadding(false);
        block.setSpacing(false);
        block.getStyle().set("gap", "4px");

        String theme = resolvePrefixTheme(prefix);
        Span badge = SnapshotUiSupport.createStatusBadge(SnapshotUiSupport.formatDimensionLabel(prefix), theme);
        badge.getStyle().set("align-self", "flex-start");

        Span textSpan = new Span(body);
        textSpan.getStyle()
                .set("font-size", "0.80rem")
                .set("font-weight", "500")
                .set("line-height", "1.35")
                .set("color", "var(--lumo-body-text-color)");

        block.add(badge, textSpan);
        return block;
    }

    private Span buildDescriptiveText(String text) {
        Span span = new Span(text);
        span.getStyle()
                .set("font-size", "0.82rem")
                .set("font-weight", "500")
                .set("line-height", "1.35")
                .set("color", "var(--lumo-header-text-color)");
        return span;
    }

    private Span buildStandardValueSpan(String rawKey, String valStr) {
        String displayVal = formatShortMetricValue(rawKey, valStr);
        Span span = new Span(!displayVal.isBlank() ? displayVal : "-");
        span.getStyle()
                .set("font-weight", "700")
                .set("color", "var(--lumo-header-text-color)")
                .set("font-size", "1.05rem")
                .set("line-height", "1.2");
        return span;
    }

    private String formatShortMetricValue(String rawKey, String valStr) {
        if (valStr == null || valStr.isBlank()) return "-";
        try {
            double d = Double.parseDouble(valStr);
            String kLower = rawKey.toLowerCase(Locale.ROOT);
            if (kLower.contains("eps") && !kLower.contains("pct") && !kLower.contains("revision") && !kLower.contains("spread")) {
                return String.format(Locale.US, "$%.2f", d);
            }
        } catch (NumberFormatException ignored) {
            // Not a pure number
        }
        return valStr;
    }

    private String extractMetricString(JsonNode valNode) {
        if (valNode == null || valNode.isNull()) return "";
        String s = valNode.isString() ? valNode.asString("") : valNode.asString();
        return s.replace("null down", "0 down").replace("null up", "0 up");
    }

    private boolean isStatusOrEnum(String key, String val) {
        if (val == null || val.isBlank()) return false;
        if (isCategoricalZone(key, val)) return true;
        String kLower = key.toLowerCase(Locale.ROOT);
        if (kLower.contains("status") || kLower.contains("flag") || kLower.contains("divergence")) {
            return true;
        }
        String trimmed = val.trim();
        return trimmed.matches("^[A-Z][A-Z0-9_]{2,}$");
    }

    private boolean isCategoricalZone(String key, String val) {
        if (key.toLowerCase(Locale.ROOT).contains("zone")) return true;
        String upper = val.toUpperCase(Locale.ROOT);
        return upper.equals("AGGRESSIVE") || upper.equals("SPECULATIVE") || upper.equals("REALISTIC")
                || upper.equals("CONSERVATIVE") || upper.equals("HIGH") || upper.equals("MODERATE");
    }

    private boolean isSentimentPrefix(String prefix) {
        String p = prefix.trim().toLowerCase(Locale.ROOT);
        return p.contains("mixed") || p.contains("negative") || p.contains("positive")
                || p.contains("bullish") || p.contains("bearish") || p.contains("neutral")
                || p.contains("warning") || p.contains("caution");
    }

    private String resolvePrefixTheme(String prefix) {
        String p = prefix.trim().toLowerCase(Locale.ROOT);
        if (p.contains("bullish") || (p.contains("positive") && !p.contains("mixed"))) {
            return "success";
        }
        if (p.contains("bearish") || (p.contains("negative") && !p.contains("mixed"))) {
            return "error";
        }
        if (p.contains("mixed") || p.contains("warning") || p.contains("caution")) {
            return "warning";
        }
        return "contrast";
    }

    private Component renderSummary(JsonNode value, String jsonData, String title) {
        if (value == null || value.asString("").isBlank()) return new Span();
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getStyle().set("margin-bottom", "20px");

        H4 header = new H4(title);
        header.getStyle()
                .set("margin-top", "24px")
                .set("margin-bottom", "16px")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "8px");
        layout.add(header);

        layout.add(markdownRenderer.renderMarkdownWithCitations(value.asString(), jsonData));
        return layout;
    }

    private Scroller wrapInScroller(Component content) {
        Scroller scroller = new Scroller(content, Scroller.ScrollDirection.VERTICAL);
        scroller.setWidthFull();
        scroller.getStyle().set("overflow-x", "hidden");
        return scroller;
    }

    private void addScenariosIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        if (agentData.has("scenarios")) {
            layout.add(renderScenariosNode(agentData.get("scenarios"), jsonData));
        }
    }

    private void addThesisBreakersIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        if (agentData.has("thesis_breakers")) {
            layout.add(renderThesisBreakersNode(agentData.get("thesis_breakers"), jsonData));
        }
    }

    private Component renderThesisBreakersNode(JsonNode breakersNode, String jsonData) {
        if (breakersNode == null || !breakersNode.isArray() || breakersNode.isEmpty()) return new Span();
        VerticalLayout container = new VerticalLayout();
        container.setPadding(true);
        container.setSpacing(true);
        container.setWidthFull();
        container.getStyle()
                .set("background", "rgba(255, 171, 0, 0.06)")
                .set("border", "1px solid rgba(255, 171, 0, 0.25)")
                .set("border-radius", "8px")
                .set("margin-top", "16px")
                .set("margin-bottom", "16px");

        container.add(createThesisBreakersHeader());
        breakersNode.forEach(item -> container.add(createThesisBreakerRow(item.asString(""), jsonData)));
        return container;
    }

    private Component createThesisBreakersHeader() {
        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.getStyle().set("gap", "8px");

        Icon warningIcon = VaadinIcon.WARNING.create();
        warningIcon.setColor("#ffb300");
        warningIcon.setSize("18px");

        H5 title = new H5("Thesis Breakers & Monitoring Triggers");
        title.getStyle().set("margin", "0").set("color", "#ffb300").set("font-weight", "bold");
        titleRow.add(warningIcon, title);
        return titleRow;
    }

    private Component createThesisBreakerRow(String itemText, String jsonData) {
        HorizontalLayout itemRow = new HorizontalLayout();
        itemRow.setAlignItems(FlexComponent.Alignment.START);
        itemRow.getStyle().set("gap", "8px");

        Span dot = new Span("•");
        dot.getStyle().set("color", "#ffb300").set("font-weight", "bold");

        Component text = markdownRenderer.renderMarkdownWithCitations(itemText, jsonData);
        text.getStyle().set("font-size", "0.9rem").set("color", "var(--lumo-body-text-color)");
        itemRow.add(dot, text);
        return itemRow;
    }

    private Component renderScenariosNode(JsonNode scenariosNode, String jsonData) {
        if (scenariosNode == null || !scenariosNode.isObject() || scenariosNode.isEmpty()) return new Span();
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(false);
        container.setWidthFull();
        container.getStyle().set("margin-top", "20px").set("margin-bottom", "20px");

        H4 headerTitle = new H4("Investment Scenarios");
        headerTitle.getStyle()
                .set("margin-top", "0")
                .set("margin-bottom", "14px")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "8px")
                .set("font-size", "1.1rem")
                .set("color", "var(--lumo-header-text-color)");
        container.add(headerTitle);

        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.getStyle().set("gap", "16px").set("flex-wrap", "wrap");

        if (scenariosNode.has("bull")) {
            cardsLayout.add(createScenarioCardNode("Bull Case", scenariosNode.get("bull"), "#81c784", "rgba(76, 175, 80, 0.08)", jsonData));
        }
        if (scenariosNode.has("base")) {
            cardsLayout.add(createScenarioCardNode("Base Case", scenariosNode.get("base"), "#64b5f6", "rgba(33, 150, 243, 0.08)", jsonData));
        }
        if (scenariosNode.has("bear")) {
            cardsLayout.add(createScenarioCardNode("Bear Case", scenariosNode.get("bear"), "#e57373", "rgba(244, 67, 54, 0.08)", jsonData));
        }

        container.add(cardsLayout);
        return container;
    }

    private Component createScenarioCardNode(String title, JsonNode caseNode, String accentColor, String bgTint, String jsonData) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
                .set("flex", "1 1 250px")
                .set("background", bgTint)
                .set("border", "1px solid " + accentColor)
                .set("border-radius", "8px");

        H5 cardTitle = new H5(title);
        cardTitle.getStyle().set("margin", "0").set("color", accentColor).set("font-weight", "bold");
        card.add(cardTitle);

        if (caseNode.has("description")) {
            Component descComp = markdownRenderer.renderMarkdownWithCitations(caseNode.path("description").asString(""), jsonData);
            descComp.getStyle().set("font-size", "0.85rem").set("line-height", "1.4");
            card.add(descComp);
        }

        if (caseNode.has("key_assumption")) {
            card.add(createScenarioAssumptionComponent(caseNode.path("key_assumption").asString(""), jsonData));
        }

        String moveText = caseNode.path("implied_upside").asString("");
        if (moveText.isBlank()) moveText = caseNode.path("implied_move").asString("");
        if (moveText.isBlank()) moveText = caseNode.path("implied_downside").asString("");

        if (!moveText.isBlank()) {
            Component moveComp = markdownRenderer.renderMarkdownWithCitations(moveText, jsonData);
            moveComp.getStyle()
                    .set("font-weight", "bold")
                    .set("color", accentColor)
                    .set("font-size", "0.9rem");
            card.add(moveComp);
        }

        return card;
    }

    private Component createScenarioAssumptionComponent(String assumptionText, String jsonData) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);
        Span titleSpan = new Span("Key Assumption:");
        titleSpan.getStyle().set("font-size", "0.8rem").set("font-weight", "600").set("color", "var(--lumo-secondary-text-color)");
        Component assumptionComp = markdownRenderer.renderMarkdownWithCitations(assumptionText, jsonData);
        assumptionComp.getStyle().set("font-size", "0.8rem").set("color", "var(--lumo-secondary-text-color)").set("margin-top", "4px");
        layout.add(titleSpan, assumptionComp);
        return layout;
    }
}
