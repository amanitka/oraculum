package com.oraculum.ui.views.components.renderer.agent;

import com.oraculum.ui.views.components.renderer.MarkdownRenderer;
import com.oraculum.ui.views.components.renderer.RendererUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AgentCustomFieldRenderer {

    private final MarkdownRenderer markdownRenderer;

    public void addCustomFieldsIfPresent(
            VerticalLayout layout,
            JsonNode agentData,
            Set<String> handledSchemaKeys,
            String jsonData
    ) {
        for (Map.Entry<String, JsonNode> field : agentData.properties()) {
            String key = field.getKey();
            if (!handledSchemaKeys.contains(key)) {
                renderCustomField(layout, key, field.getValue(), jsonData);
            }
        }
    }

    private void renderCustomField(VerticalLayout layout, String key, JsonNode value, String jsonData) {
        if ("is_consistent".equals(key) || "isConsistent".equals(key)) {
            layout.add(buildConsistencyRow(value.asBoolean(true)));
            return;
        }
        layout.add(buildCustomFieldHeader(key));
        layout.add(renderFieldValue(key, value, jsonData));
    }

    private HorizontalLayout buildConsistencyRow(boolean isConsistent) {
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
        return consistencyRow;
    }

    private H3 buildCustomFieldHeader(String key) {
        H3 fieldHeader = new H3(RendererUtil.formatKeyTitle(key));
        fieldHeader.getStyle()
                .set("margin-top", "2rem")
                .set("margin-bottom", "1rem")
                .set("font-size", "1.1rem")
                .set("font-weight", "600")
                .set("color", "var(--lumo-header-text-color)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "0.5rem");
        return fieldHeader;
    }

    private Component renderFieldValue(String key, JsonNode value, String jsonData) {
        if (value.isArray()) {
            return renderArrayField(key, value, jsonData);
        }
        if (value.isObject()) {
            return renderObjectField(value);
        }
        return renderPrimitiveField(value, jsonData);
    }

    private Component renderArrayField(String key, JsonNode value, String jsonData) {
        if (value == null || value.isEmpty()) {
            Span emptySpan = new Span("None recorded");
            emptySpan.getStyle().set("color", "var(--lumo-tertiary-text-color)")
                    .set("font-style", "italic")
                    .set("font-size", "0.9rem");
            return emptySpan;
        }
        UnorderedList list = new UnorderedList();
        list.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.FontSize.SMALL);
        list.getStyle().set("list-style-type", "none").set("padding-left", "0");
        for (JsonNode item : value) {
            list.add(buildArrayListItem(key, item, jsonData));
        }
        return list;
    }

    private ListItem buildArrayListItem(String key, JsonNode item, String jsonData) {
        if (!item.isObject()) {
            ListItem li = new ListItem();
            li.add(markdownRenderer.renderMarkdownWithCitations(item.asString(), jsonData));
            return li;
        }
        if ("recommended_reruns".equals(key)) {
            return new ListItem(renderRecommendedRerunItem(item));
        }
        if ("contradictions_found".equals(key) || "contradictions".equals(key)) {
            return new ListItem(renderContradictionItem(item));
        }
        return new ListItem(renderObjectField(item));
    }

    private Component renderRecommendedRerunItem(JsonNode item) {
        Div rerunDiv = new Div();
        rerunDiv.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.Padding.SMALL,
                LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.Bottom.SMALL);

        String specialist = item.path("specialist").asString("");
        String severity = item.path("severity").asString("");
        Span badge = new Span(specialist + " (Severity " + severity + ")");
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add("error");
        badge.addClassNames(LumoUtility.Margin.Bottom.SMALL);
        badge.getStyle().set("display", "inline-block");

        Paragraph inst = new Paragraph(item.path("instruction").asString(""));
        inst.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.SMALL);

        rerunDiv.add(badge, inst);
        return rerunDiv;
    }

    private Component renderContradictionItem(JsonNode item) {
        Div contradictionDiv = createContradictionContainer();
        HorizontalLayout badgeRow = buildContradictionBadges(item);

        Paragraph descPara = new Paragraph(item.path("description").asString(""));
        descPara.getStyle()
                .set("margin", "0")
                .set("font-size", "0.95rem")
                .set("line-height", "1.6")
                .set("color", "var(--lumo-body-text-color)");

        contradictionDiv.add(badgeRow, descPara);
        return contradictionDiv;
    }

    private Div createContradictionContainer() {
        Div div = new Div();
        div.getStyle()
                .set("background", "rgba(var(--lumo-contrast-rgb), 0.03)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-left", "4px solid var(--lumo-warning-color)")
                .set("border-radius", "0 8px 8px 0")
                .set("padding", "16px 20px")
                .set("margin-bottom", "16px");
        return div;
    }

    private HorizontalLayout buildContradictionBadges(JsonNode item) {
        HorizontalLayout badgeRow = new HorizontalLayout();
        badgeRow.setAlignItems(FlexComponent.Alignment.CENTER);
        badgeRow.getStyle().set("gap", "10px").set("flex-wrap", "wrap").set("margin-bottom", "12px");

        addResolutionBadgeIfPresent(badgeRow, item.path("resolution").asString(""));
        addCorrectionTypeBadgeIfPresent(badgeRow, item.path("correction_type").asString(""));
        addAgentBadgesIfPresent(badgeRow, item.path("agents_involved"));
        return badgeRow;
    }

    private void addResolutionBadgeIfPresent(HorizontalLayout badgeRow, String resolution) {
        if (resolution.isBlank()) {
            return;
        }
        Span resBadge = new Span(resolution.replace("_", " "));
        resBadge.getElement().getThemeList().add("badge");
        String theme = "UNRESOLVED".equalsIgnoreCase(resolution)
                ? "error"
                : ("RERUN_APPLIED".equalsIgnoreCase(resolution) ? "contrast" : "success");
        resBadge.getElement().getThemeList().add(theme);
        resBadge.getStyle().set("font-weight", "600").set("padding", "4px 10px");
        badgeRow.add(resBadge);
    }

    private void addCorrectionTypeBadgeIfPresent(HorizontalLayout badgeRow, String correctionType) {
        if (correctionType.isBlank()) {
            return;
        }
        Span typeBadge = new Span(correctionType.replace("_", " "));
        typeBadge.getElement().getThemeList().add("badge");
        typeBadge.getElement().getThemeList().add("primary");
        typeBadge.getStyle().set("font-weight", "600").set("padding", "4px 10px");
        badgeRow.add(typeBadge);
    }

    private void addAgentBadgesIfPresent(HorizontalLayout badgeRow, JsonNode agentsNode) {
        if (!agentsNode.isArray() || agentsNode.isEmpty()) {
            return;
        }
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
            layout.add(buildObjectEntryRow(entry.getKey(), entry.getValue()));
        }
        container.add(layout);
        return container;
    }

    private HorizontalLayout buildObjectEntryRow(String rawKey, JsonNode val) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.BASELINE);

        Span label = new Span(RendererUtil.formatKeyTitle(rawKey) + ": ");
        label.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);
        label.getStyle().set("min-width", "130px");

        Component valComp;
        if (val.isArray()) {
            valComp = renderArrayField(rawKey, val, null);
        } else if (val.isObject()) {
            valComp = renderObjectField(val);
        } else {
            Span valSpan = new Span(val.asString());
            valSpan.addClassNames(LumoUtility.FontSize.SMALL);
            valComp = valSpan;
        }
        row.add(label, valComp);
        return row;
    }

    private Component renderPrimitiveField(JsonNode value, String jsonData) {
        return markdownRenderer.renderMarkdownWithCitations(value.asString(), jsonData);
    }
}
