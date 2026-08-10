package com.oraculum.ui.views.components.renderer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AgentDataRenderer {

    private static final Set<String> HANDLED_SCHEMA_KEYS = Set.of(
            "headline", "score", "confidence", "confidence_reasons", "recommendation",
            "outlook", "conviction", "management_sentiment", "sentiment", "managementSentiment",
            "strengths", "key_drivers", "weaknesses", "key_risks", "evidence",
            "metrics", "factor_scores", "summary", "executive_summary", "recommendation_reasoning",
            "thesis_breakers", "scenarios", "what_would_change_this", "thesis", "top_bull_points", "top_bear_points", "valuation_one_liner"
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
        addTilesRowIfPresent(layout, agentData);
        addStrengthsIfPresent(layout, agentData, jsonData);
        addWeaknessesIfPresent(layout, agentData, jsonData);
        addScenariosIfPresent(layout, agentData);
        addThesisBreakersIfPresent(layout, agentData);
        addEvidenceIfPresent(layout, agentData, jsonData);
        addMetricsIfPresent(layout, agentData);
        addSummaryIfPresent(layout, agentData, jsonData);
        addCustomFieldsIfPresent(layout, agentData, jsonData);

        return wrapInScroller(layout);
    }

    private void addHeadlineIfPresent(VerticalLayout layout, JsonNode agentData) {
        if (agentData.has("headline")) {
            layout.add(renderHeadline(agentData.get("headline")));
        }
    }

    private void addTilesRowIfPresent(VerticalLayout layout, JsonNode agentData) {
        HorizontalLayout tilesRow = buildTilesRow(agentData);
        if (tilesRow != null) {
            layout.add(tilesRow);
        }
    }

    private HorizontalLayout buildTilesRow(JsonNode agentData) {
        HorizontalLayout tilesRow = new HorizontalLayout();
        tilesRow.setWidthFull();
        tilesRow.getStyle().set("gap", "16px").set("margin-bottom", "20px").set("flex-wrap", "wrap");
        boolean hasTiles = false;

        if (agentData.has("score")) {
            tilesRow.add(createTile("Score", renderScoreContent(agentData.get("score"))));
            hasTiles = true;
        }
        if (agentData.has("confidence")) {
            tilesRow.add(createTile("Confidence", renderConfidenceContent(agentData.get("confidence"), agentData.path("confidence_reasons"))));
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
        }
        if (agentData.has("management_sentiment")) {
            tilesRow.add(createTile("Sentiment", renderSentimentBadge(agentData.get("management_sentiment").asString())));
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

    private void addEvidenceIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        if (agentData.has("evidence")) {
            layout.add(renderEvidence(agentData.get("evidence"), jsonData));
        }
    }

    private void addMetricsIfPresent(VerticalLayout layout, JsonNode agentData) {
        if (agentData.has("metrics")) {
            layout.add(renderMetrics(agentData.get("metrics"), "Key Metrics"));
        } else if (agentData.has("factor_scores")) {
            layout.add(renderMetrics(agentData.get("factor_scores"), "Factor Scores"));
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

            Span statusBadge = new Span(isConsistent ? "✔ CONSISTENT" : "⚠️ CONTRADICTION DETECTED");
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

    private Component renderScoreContent(JsonNode value) {
        double score = value.asDouble(0.0);
        if (score > 10.0) {
            score = score / 10.0;
        }
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.getStyle().set("gap", "12px");

        HorizontalLayout meter = new HorizontalLayout();
        meter.setSpacing(false);
        meter.getStyle().set("gap", "3px").set("align-items", "center");

        int fullBlocks = (int) score;
        for (int i = 1; i <= 10; i++) {
            Span bar = new Span();
            bar.getStyle()
                    .set("width", "12px")
                    .set("height", "16px")
                    .set("border-radius", "2px");
            if (i <= fullBlocks) {
                if (i <= 3) {
                    bar.getStyle().set("background", "var(--lumo-error-color)");
                } else if (i <= 7) {
                    bar.getStyle().set("background", "var(--lumo-warning-color)");
                } else {
                    bar.getStyle().set("background", "var(--lumo-success-color)");
                }
            } else {
                bar.getStyle().set("background", "rgba(var(--lumo-contrast-rgb), 0.1)");
            }
            meter.add(bar);
        }

        Span text = new Span(String.format(Locale.US, "%.1f / 10.0", score));
        text.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);

        layout.add(meter, text);
        return layout;
    }

    private Component renderConfidenceContent(JsonNode confidence, JsonNode reasons) {
        VerticalLayout main = new VerticalLayout();
        main.setPadding(false);
        main.setSpacing(false);
        main.getStyle().set("gap", "12px");

        double confVal = confidence.asDouble(0.0);
        String badgeText;
        if (confVal > 1.0) {
            badgeText = (int) confVal + "%";
            confVal = confVal / 100.0;
        } else {
            badgeText = (int) (confVal * 100) + "%";
        }
        Span badge = new Span(badgeText);
        badge.getElement().getThemeList().add("badge");
        if (confVal >= 0.8) badge.getElement().getThemeList().add("success");
        else if (confVal >= 0.5) badge.getElement().getThemeList().add("contrast");
        else badge.getElement().getThemeList().add("error");

        main.add(badge);

        if (reasons != null && reasons.isArray() && !reasons.isEmpty()) {
            UnorderedList reasonsList = new UnorderedList();
            reasonsList.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "0.85rem").set("margin", "0").set("padding-left", "16px");
            for (JsonNode reason : reasons) {
                reasonsList.add(new ListItem(reason.asString()));
            }
            main.add(reasonsList);
        }
        return main;
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
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addComponentColumn(item -> {
            Span currentSpan = new Span(item.path("value").asString(""));
            currentSpan.getStyle().set("font-weight", "bold").set("color", "var(--lumo-header-text-color)");
            return currentSpan;
        }).setHeader("Current").setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(item -> {
            String prev = item.path("previous_value").asString("");
            Span prevSpan = new Span(prev.isBlank() ? "-" : prev);
            prevSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
            return prevSpan;
        }).setHeader("Previous").setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(item -> renderTrendIcon(item.path("trend").asString("")))
                .setHeader("Trend")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(item -> {
            String sourceType = item.path("source_type").asString("");
            if (sourceType.isBlank()) return new Span("-");
            Span badge = new Span(sourceType);
            badge.getElement().getThemeList().add("badge");
            badge.getStyle().set("font-size", "0.75rem").set("font-weight", "600");
            switch (sourceType.toUpperCase()) {
                case "REPORTED" -> {
                    badge.getElement().getThemeList().add("primary");
                    badge.getStyle().set("background-color", "rgba(50, 150, 250, 0.15)").set("color", "#4fc3f7");
                }
                case "CALCULATED" -> {
                    badge.getStyle().set("background-color", "rgba(156, 39, 176, 0.15)").set("color", "#ba68c8");
                }
                case "DERIVED" -> {
                    badge.getElement().getThemeList().add("contrast");
                    badge.getStyle().set("background-color", "rgba(255, 152, 0, 0.15)").set("color", "#ffb74d");
                }
                case "ESTIMATED" -> {
                    badge.getElement().getThemeList().add("success");
                    badge.getStyle().set("background-color", "rgba(76, 175, 80, 0.15)").set("color", "#81c784");
                }
                default -> badge.getElement().getThemeList().add("contrast");
            }
            return badge;
        }).setHeader("Type").setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(item -> {
            String sig = item.path("significance").asString("");
            if (sig.isBlank()) return new Span("-");

            Component sigComp = markdownRenderer.renderMarkdownWithCitations(sig, jsonData);
            sigComp.getStyle().set("font-size", "0.85rem").set("line-height", "1.4");

            if (sig.length() <= 65) {
                return sigComp;
            }

            String summaryText = sig.substring(0, 60) + "…";
            Details details = new Details(summaryText, sigComp);
            details.getStyle()
                    .set("font-size", "0.85rem")
                    .set("margin", "0")
                    .set("padding", "0");
            return details;
        }).setHeader("Why It Matters").setFlexGrow(3);

        List<JsonNode> itemList = new ArrayList<>();
        value.forEach(itemList::add);
        grid.setItems(itemList);
        grid.setAllRowsVisible(true);

        layout.add(grid);
        return layout;
    }

    private Component renderTrendIcon(String trend) {
        String upper = trend != null ? trend.toUpperCase() : "";
        com.vaadin.flow.component.icon.Icon icon;

        if (upper.contains("IMPROV") || upper.contains("UP") || upper.contains("BULL")) {
            icon = VaadinIcon.ARROW_UP.create();
            icon.setColor("var(--lumo-success-color)");
            icon.getStyle().set("color", "var(--lumo-success-color)");
        } else if (upper.contains("DETERIORAT") || upper.contains("DOWN") || upper.contains("DECLIN") || upper.contains("BEAR")) {
            icon = VaadinIcon.ARROW_DOWN.create();
            icon.setColor("var(--lumo-error-color)");
            icon.getStyle().set("color", "var(--lumo-error-color)");
        } else {
            icon = VaadinIcon.ARROW_RIGHT.create();
            icon.setColor("#8c9197");
            icon.getStyle().set("color", "#8c9197");
        }

        icon.setSize("14px");
        return icon;
    }

    private Component renderMetrics(JsonNode value, String title) {
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

        HorizontalLayout flex = new HorizontalLayout();
        flex.setWidthFull();
        flex.getStyle().set("gap", "12px").set("flex-wrap", "wrap");

        for (Map.Entry<String, JsonNode> entry : value.properties()) {
            Div chip = new Div();
            chip.getStyle()
                    .set("background", "rgba(var(--lumo-contrast-rgb), 0.04)")
                    .set("border", "1px solid var(--lumo-contrast-15pct)")
                    .set("border-radius", "6px")
                    .set("padding", "8px 12px")
                    .set("font-size", "0.85rem");

            Span keySpan = new Span(RendererUtil.formatKeyTitle(entry.getKey()) + ": ");
            keySpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

            Span valSpan = new Span(entry.getValue().asString());
            valSpan.getStyle().set("font-weight", "bold").set("color", "var(--lumo-primary-text-color)");

            chip.add(keySpan, valSpan);
            flex.add(chip);
        }
        layout.add(flex);
        return layout;
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

    private void addScenariosIfPresent(VerticalLayout layout, JsonNode agentData) {
        if (agentData.has("scenarios")) {
            layout.add(renderScenariosNode(agentData.get("scenarios")));
        }
    }

    private void addThesisBreakersIfPresent(VerticalLayout layout, JsonNode agentData) {
        if (agentData.has("thesis_breakers")) {
            layout.add(renderThesisBreakersNode(agentData.get("thesis_breakers")));
        }
    }

    private Component renderThesisBreakersNode(JsonNode breakersNode) {
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

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.getStyle().set("gap", "8px");

        com.vaadin.flow.component.icon.Icon warningIcon = VaadinIcon.WARNING.create();
        warningIcon.setColor("#ffb300");
        warningIcon.setSize("18px");

        H5 title = new H5("Thesis Breakers & Monitoring Triggers");
        title.getStyle().set("margin", "0").set("color", "#ffb300").set("font-weight", "bold");
        titleRow.add(warningIcon, title);
        container.add(titleRow);

        breakersNode.forEach(item -> {
            HorizontalLayout itemRow = new HorizontalLayout();
            itemRow.setAlignItems(FlexComponent.Alignment.START);
            itemRow.getStyle().set("gap", "8px");

            Span dot = new Span("•");
            dot.getStyle().set("color", "#ffb300").set("font-weight", "bold");

            Span text = new Span(item.asString(""));
            text.getStyle().set("font-size", "0.9rem").set("color", "var(--lumo-body-text-color)");
            itemRow.add(dot, text);
            container.add(itemRow);
        });

        return container;
    }

    private Component renderScenariosNode(JsonNode scenariosNode) {
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
            cardsLayout.add(createScenarioCardNode("Bull Case", scenariosNode.get("bull"), "#81c784", "rgba(76, 175, 80, 0.08)"));
        }
        if (scenariosNode.has("base")) {
            cardsLayout.add(createScenarioCardNode("Base Case", scenariosNode.get("base"), "#64b5f6", "rgba(33, 150, 243, 0.08)"));
        }
        if (scenariosNode.has("bear")) {
            cardsLayout.add(createScenarioCardNode("Bear Case", scenariosNode.get("bear"), "#e57373", "rgba(244, 67, 54, 0.08)"));
        }

        container.add(cardsLayout);
        return container;
    }

    private Component createScenarioCardNode(String title, JsonNode caseNode, String accentColor, String bgTint) {
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
            Span desc = new Span(caseNode.path("description").asString(""));
            desc.getStyle().set("font-size", "0.85rem").set("line-height", "1.4");
            card.add(desc);
        }

        if (caseNode.has("key_assumption")) {
            Div assumptionBox = new Div();
            assumptionBox.getStyle().set("font-size", "0.8rem").set("color", "var(--lumo-secondary-text-color)");
            assumptionBox.setText("Key Assumption: " + caseNode.path("key_assumption").asString(""));
            card.add(assumptionBox);
        }

        String moveText = caseNode.path("implied_upside").asString("");
        if (moveText.isBlank()) moveText = caseNode.path("implied_move").asString("");
        if (moveText.isBlank()) moveText = caseNode.path("implied_downside").asString("");

        if (!moveText.isBlank()) {
            Span moveSpan = new Span(moveText);
            moveSpan.getStyle()
                    .set("font-weight", "bold")
                    .set("color", accentColor)
                    .set("font-size", "0.9rem");
            card.add(moveSpan);
        }

        return card;
    }
}
