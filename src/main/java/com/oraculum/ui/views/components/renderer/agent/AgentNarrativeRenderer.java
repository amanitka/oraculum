package com.oraculum.ui.views.components.renderer.agent;

import com.oraculum.ui.views.components.renderer.MarkdownRenderer;
import com.oraculum.ui.views.components.renderer.SnapshotUiSupport;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AgentNarrativeRenderer {

    private final MarkdownRenderer markdownRenderer;

    public void addHeadlineIfPresent(VerticalLayout layout, JsonNode agentData) {
        if (agentData.has("headline")) {
            layout.add(renderHeadline(agentData.get("headline")));
        }
    }

    public void addStrengthsIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        if (agentData.has("strengths")) {
            layout.add(renderPositiveList(agentData.get("strengths"), "Key Strengths", jsonData));
        } else if (agentData.has("key_drivers")) {
            layout.add(renderPositiveList(agentData.get("key_drivers"), "Key Drivers", jsonData));
        }
    }

    public void addWeaknessesIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        if (agentData.has("weaknesses")) {
            layout.add(renderWarningList(agentData.get("weaknesses"), "Key Weaknesses", jsonData));
        } else if (agentData.has("key_risks")) {
            layout.add(renderWarningList(agentData.get("key_risks"), "Key Risks", jsonData));
        }
    }

    public void addRiskBucketsIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        if (agentData.has("business_risks")) {
            layout.add(renderWarningList(agentData.get("business_risks"), "Business Risks", jsonData));
        }
        if (agentData.has("financial_risks")) {
            layout.add(renderWarningList(agentData.get("financial_risks"), "Financial Risks", jsonData));
        }
        if (agentData.has("macro_risks")) {
            layout.add(renderWarningList(agentData.get("macro_risks"), "Macro Risks", jsonData));
        }
        if (agentData.has("sentiment_signals")) {
            layout.add(renderInfoList(agentData.get("sentiment_signals"), "Sentiment Signals", jsonData));
        }
    }

    public void addScenariosIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        if (agentData.has("scenarios")) {
            layout.add(renderScenariosNode(agentData.get("scenarios"), jsonData));
        }
    }

    public void addThesisBreakersIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        if (agentData.has("thesis_breakers")) {
            layout.add(renderThesisBreakersNode(agentData.get("thesis_breakers"), jsonData));
        }
    }

    public void addSummaryIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        if (agentData.has("summary")) {
            layout.add(renderSummary(agentData.get("summary"), jsonData, "Summary"));
        } else if (agentData.has("executive_summary")) {
            layout.add(renderSummary(agentData.get("executive_summary"), jsonData, "Executive Summary"));
        }
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

    private Component renderPositiveList(JsonNode value, String title, String jsonData) {
        return buildIconListSection(value, title, jsonData, () -> createListIcon(VaadinIcon.CHECK_CIRCLE, "var(--lumo-success-color)"));
    }

    private Component renderWarningList(JsonNode value, String title, String jsonData) {
        return buildIconListSection(value, title, jsonData, () -> createListIcon(VaadinIcon.WARNING, "var(--lumo-error-color)"));
    }

    private Component renderInfoList(JsonNode value, String title, String jsonData) {
        return buildIconListSection(value, title, jsonData, () -> createListIcon(VaadinIcon.INFO_CIRCLE, "var(--lumo-primary-color)"));
    }

    private Component buildIconListSection(JsonNode value, String title, String jsonData, Supplier<Icon> iconSupplier) {
        if (value == null || value.isEmpty()) {
            return new Span();
        }
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getStyle().set("margin-bottom", "20px");
        layout.add(SnapshotUiSupport.createSectionHeader(title));

        for (JsonNode item : value) {
            layout.add(buildIconListRow(item.asString(), jsonData, iconSupplier.get()));
        }
        return layout;
    }

    private HorizontalLayout buildIconListRow(String text, String jsonData, Icon icon) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.START);
        row.getStyle().set("margin-bottom", "16px");
        Component textComp = markdownRenderer.renderMarkdownWithCitations(text, jsonData);
        textComp.getStyle().set("line-height", "1.6");
        row.add(icon, textComp);
        return row;
    }

    private Icon createListIcon(VaadinIcon vaadinIcon, String color) {
        Icon icon = vaadinIcon.create();
        icon.setColor(color);
        icon.setSize("16px");
        icon.getStyle().set("margin-top", "4px").set("flex-shrink", "0");
        return icon;
    }

    private Component renderSummary(JsonNode value, String jsonData, String title) {
        if (value == null || value.asString("").isBlank()) {
            return new Span();
        }
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getStyle().set("margin-bottom", "20px");
        layout.add(SnapshotUiSupport.createSectionHeader(title));
        layout.add(markdownRenderer.renderMarkdownWithCitations(value.asString(), jsonData));
        return layout;
    }

    private Component renderThesisBreakersNode(JsonNode breakersNode, String jsonData) {
        if (breakersNode == null || !breakersNode.isArray() || breakersNode.isEmpty()) {
            return new Span();
        }
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
        if (scenariosNode == null || !scenariosNode.isObject() || scenariosNode.isEmpty()) {
            return new Span();
        }
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(false);
        container.setWidthFull();
        container.getStyle().set("margin-top", "20px").set("margin-bottom", "20px");

        var header = SnapshotUiSupport.createSectionHeader("Investment Scenarios");
        header.getStyle().set("margin-top", "0");
        container.add(header, buildScenarioCardsRow(scenariosNode, jsonData));
        return container;
    }

    private HorizontalLayout buildScenarioCardsRow(JsonNode scenariosNode, String jsonData) {
        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.getStyle().set("gap", "16px").set("flex-wrap", "wrap");

        if (scenariosNode.has("bull")) {
            VerticalLayout shell = createScenarioCardShell("Bull Case", "#81c784", "rgba(76, 175, 80, 0.08)");
            cardsLayout.add(populateScenarioBody(shell, scenariosNode.get("bull"), "#81c784", jsonData));
        }
        if (scenariosNode.has("base")) {
            VerticalLayout shell = createScenarioCardShell("Base Case", "#64b5f6", "rgba(33, 150, 243, 0.08)");
            cardsLayout.add(populateScenarioBody(shell, scenariosNode.get("base"), "#64b5f6", jsonData));
        }
        if (scenariosNode.has("bear")) {
            VerticalLayout shell = createScenarioCardShell("Bear Case", "#e57373", "rgba(244, 67, 54, 0.08)");
            cardsLayout.add(populateScenarioBody(shell, scenariosNode.get("bear"), "#e57373", jsonData));
        }
        return cardsLayout;
    }

    private VerticalLayout createScenarioCardShell(String title, String accentColor, String bgTint) {
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
        return card;
    }

    private Component populateScenarioBody(VerticalLayout card, JsonNode caseNode, String accentColor, String jsonData) {
        if (caseNode.has("description")) {
            Component descComp = markdownRenderer.renderMarkdownWithCitations(caseNode.path("description").asString(""), jsonData);
            descComp.getStyle().set("font-size", "0.85rem").set("line-height", "1.4");
            card.add(descComp);
        }
        if (caseNode.has("key_assumption")) {
            card.add(createScenarioAssumptionComponent(caseNode.path("key_assumption").asString(""), jsonData));
        }
        String moveText = resolveScenarioMoveText(caseNode);
        if (!moveText.isBlank()) {
            Component moveComp = markdownRenderer.renderMarkdownWithCitations(moveText, jsonData);
            moveComp.getStyle().set("font-weight", "bold").set("color", accentColor).set("font-size", "0.9rem");
            card.add(moveComp);
        }
        return card;
    }

    private String resolveScenarioMoveText(JsonNode caseNode) {
        String moveText = caseNode.path("implied_upside").asString("");
        if (moveText.isBlank()) moveText = caseNode.path("implied_move").asString("");
        if (moveText.isBlank()) moveText = caseNode.path("implied_downside").asString("");
        return moveText;
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
