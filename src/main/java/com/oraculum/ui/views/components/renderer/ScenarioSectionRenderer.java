package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.ScenariosResult;
import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Renders the Scenarios and Thesis Breakers tab content for a company analysis.
 */
@Service
@RequiredArgsConstructor
public class ScenarioSectionRenderer {

    private final MarkdownRenderer markdownRenderer;

    public Component renderScenariosTab(AnalysisResult snapshot, CompanyAnalysisDto analysis) {
        if (snapshot == null) {
            return null;
        }
        Div container = new Div();
        container.addClassNames(LumoUtility.Width.FULL);

        if (snapshot.scenarios() != null) {
            container.add(createSnapshotScenarios(snapshot.scenarios(), analysis));
        }
        if (snapshot.thesisBreakers() != null && !snapshot.thesisBreakers().isEmpty()) {
            container.add(createSnapshotThesisBreakers(snapshot.thesisBreakers(), analysis));
        }
        return container;
    }

    private Component createSnapshotThesisBreakers(List<String> thesisBreakers, CompanyAnalysisDto analysis) {
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

        container.add(buildThesisBreakersTitleRow());
        String jsonData = analysis != null ? analysis.getAnalysisData() : null;
        for (String breaker : thesisBreakers) {
            container.add(buildBreakerRow(breaker, jsonData));
        }
        return container;
    }

    private HorizontalLayout buildThesisBreakersTitleRow() {
        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.getStyle().set("gap", "8px");

        Icon warningIcon = VaadinIcon.WARNING.create();
        warningIcon.setColor(SnapshotUiSupport.COLOR_AMBER);
        warningIcon.setSize("18px");

        H5 title = new H5("Thesis Breakers & Monitoring Triggers");
        title.getStyle().set("margin", "0").set("color", SnapshotUiSupport.COLOR_AMBER).set("font-weight", "bold");
        titleRow.add(warningIcon, title);
        return titleRow;
    }

    private HorizontalLayout buildBreakerRow(String breaker, String jsonData) {
        HorizontalLayout itemRow = new HorizontalLayout();
        itemRow.setAlignItems(FlexComponent.Alignment.START);
        itemRow.getStyle().set("gap", "8px");

        Span dot = new Span("•");
        dot.getStyle().set("color", SnapshotUiSupport.COLOR_AMBER).set("font-weight", "bold");

        Component textComp = markdownRenderer.renderMarkdownWithCitations(breaker, jsonData);
        textComp.getStyle().set("font-size", "0.9rem").set("color", "var(--lumo-body-text-color)");
        itemRow.add(dot, textComp);
        return itemRow;
    }

    private Component createSnapshotScenarios(ScenariosResult scenarios, CompanyAnalysisDto analysis) {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(false);
        container.setWidthFull();
        container.getStyle().set("margin-top", "0").set("margin-bottom", "20px");

        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.getStyle().set("gap", "16px").set("flex-wrap", "wrap");

        if (scenarios.bull() != null) {
            cardsLayout.add(createScenarioCard("Bull Case", scenarios.bull().description(), scenarios.bull().keyAssumption(),
                    scenarios.bull().impliedUpside(), SnapshotUiSupport.COLOR_GREEN, "rgba(46, 204, 113, 0.05)", "rgba(46, 204, 113, 0.2)", analysis));
        }
        if (scenarios.base() != null) {
            cardsLayout.add(createScenarioCard("Base Case", scenarios.base().description(), scenarios.base().keyAssumption(),
                    scenarios.base().impliedMove(), SnapshotUiSupport.COLOR_BLUE, "rgba(52, 152, 219, 0.05)", "rgba(52, 152, 219, 0.2)", analysis));
        }
        if (scenarios.bear() != null) {
            cardsLayout.add(createScenarioCard("Bear Case", scenarios.bear().description(), scenarios.bear().keyAssumption(),
                    scenarios.bear().impliedDownside(), SnapshotUiSupport.COLOR_RED, "rgba(231, 76, 60, 0.05)", "rgba(231, 76, 60, 0.2)", analysis));
        }
        container.add(cardsLayout);
        return container;
    }

    private Component createScenarioCard(String title, String descText, String assumptionText, String moveText,
                                         String accentColor, String bgTint, String borderColor, CompanyAnalysisDto analysis) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
                .set("flex", "1 1 250px")
                .set("background", bgTint)
                .set("border", "1px solid " + borderColor)
                .set("border-radius", "8px");

        H5 cardTitle = new H5(title);
        cardTitle.getStyle().set("margin", "0").set("color", accentColor).set("font-weight", "bold");
        card.add(cardTitle);

        String jsonData = analysis != null ? analysis.getAnalysisData() : null;
        addScenarioText(card, descText, jsonData);
        addScenarioAssumption(card, assumptionText, jsonData);
        addScenarioMove(card, moveText, accentColor, jsonData);
        return card;
    }

    private void addScenarioText(VerticalLayout card, String descText, String jsonData) {
        if (descText != null && !descText.isBlank()) {
            Component descComp = markdownRenderer.renderMarkdownWithCitations(descText, jsonData);
            descComp.getStyle().set("font-size", "0.85rem").set("line-height", "1.4");
            card.add(descComp);
        }
    }

    private void addScenarioAssumption(VerticalLayout card, String assumptionText, String jsonData) {
        if (assumptionText != null && !assumptionText.isBlank()) {
            VerticalLayout assumptionLayout = new VerticalLayout();
            assumptionLayout.setSpacing(false);
            assumptionLayout.setPadding(false);
            Span titleSpan = new Span("Key Assumption:");
            titleSpan.getStyle().set("font-size", "0.8rem").set("font-weight", "600").set("color", "var(--lumo-secondary-text-color)");
            Component assumptionComp = markdownRenderer.renderMarkdownWithCitations(assumptionText, jsonData);
            assumptionComp.getStyle().set("font-size", "0.8rem").set("color", "var(--lumo-secondary-text-color)").set("margin-top", "4px");
            assumptionLayout.add(titleSpan, assumptionComp);
            card.add(assumptionLayout);
        }
    }

    private void addScenarioMove(VerticalLayout card, String moveText, String accentColor, String jsonData) {
        if (moveText != null && !moveText.isBlank()) {
            Component moveComp = markdownRenderer.renderMarkdownWithCitations(moveText, jsonData);
            moveComp.getStyle().set("font-weight", "bold").set("color", accentColor).set("font-size", "0.9rem");
            card.add(moveComp);
        }
    }
}
