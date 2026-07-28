package com.oraculum.ui.views.components.renderer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentDataRenderer {

    private final MarkdownRenderer markdownRenderer;

    public Component createAgentTabContent(JsonNode agentData, String jsonData) {
        if (agentData == null || agentData.properties().isEmpty()) {
            return null;
        }

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setSizeFull();
        layout.getStyle().set("max-width", "100%").set("box-sizing", "border-box").set("margin", "0 auto").set("overflow-y", "auto");

        for (Map.Entry<String, JsonNode> field : agentData.properties()) {
            String key = field.getKey();
            JsonNode value = field.getValue();

            switch (key) {
                case "is_consistent", "isConsistent" -> {
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
                            .set("color", "var(--lumo-secondary-text-color)")
                            .set("text-transform", "uppercase");

                    Span statusBadge = new Span(isConsistent ? "✔ CONSISTENT" : "⚠️ CONTRADICTION DETECTED");
                    statusBadge.getElement().getThemeList().add("badge");
                    statusBadge.getElement().getThemeList().add(isConsistent ? "success" : "error");
                    statusBadge.getStyle().set("font-size", "0.85rem").set("padding", "4px 10px");

                    consistencyRow.add(label, statusBadge);
                    layout.add(consistencyRow);
                }
                case "management_sentiment", "managementSentiment", "sentiment" -> {
                    HorizontalLayout row = new HorizontalLayout();
                    row.setAlignItems(FlexComponent.Alignment.CENTER);
                    row.getStyle().set("margin-top", "20px").set("margin-bottom", "12px").set("gap", "12px");

                    H4 label = new H4(RendererUtil.formatKeyTitle(key) + ":");
                    label.getStyle()
                            .set("margin", "0")
                            .set("font-size", "0.9rem")
                            .set("font-weight", "700")
                            .set("letter-spacing", "0.5px")
                            .set("color", "var(--lumo-secondary-text-color)")
                            .set("text-transform", "uppercase");

                    row.add(label, renderSentimentBadge(value.asString()));
                    layout.add(row);
                }
                case "bullish_conviction", "bullishConviction" -> {
                    HorizontalLayout row = new HorizontalLayout();
                    row.setAlignItems(FlexComponent.Alignment.CENTER);
                    row.getStyle().set("margin-top", "20px").set("margin-bottom", "12px").set("gap", "12px");

                    H4 label = new H4(RendererUtil.formatKeyTitle(key) + ":");
                    label.getStyle()
                            .set("margin", "0")
                            .set("font-size", "0.9rem")
                            .set("font-weight", "700")
                            .set("letter-spacing", "0.5px")
                            .set("color", "var(--lumo-secondary-text-color)")
                            .set("text-transform", "uppercase");

                    row.add(label, renderConvictionMeter(value.asInt(0)));
                    layout.add(row);
                }
                case null, default -> {
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
            }
        }

        return layout;
    }

    private Component renderSentimentBadge(String sentimentStr) {
        if (sentimentStr == null || sentimentStr.isBlank()) {
            return new Span("N/A");
        }
        String labelText = sentimentStr.replace("_", " ").toUpperCase();
        Span badge = new Span(labelText);
        badge.getElement().getThemeList().add("badge");

        if (sentimentStr.contains("BULLISH") || sentimentStr.contains("BUY")) {
            badge.getElement().getThemeList().add("success");
        } else if (sentimentStr.contains("BEARISH") || sentimentStr.contains("SELL")) {
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
}
