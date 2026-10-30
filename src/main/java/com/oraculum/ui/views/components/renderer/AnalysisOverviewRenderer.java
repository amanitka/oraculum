package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.oraculum.analyst.api.dto.AnalysisResult.ScenariosResult;
import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AnalysisOverviewRenderer {

    private final MarkdownRenderer markdownRenderer;
    private final DecisionSnapshotRenderer decisionSnapshotRenderer;
    private final VerdictHeroRenderer verdictHeroRenderer;

    @Autowired
    public AnalysisOverviewRenderer(MarkdownRenderer markdownRenderer,
                                    DecisionSnapshotRenderer decisionSnapshotRenderer,
                                    VerdictHeroRenderer verdictHeroRenderer) {
        this.markdownRenderer = markdownRenderer;
        this.decisionSnapshotRenderer = decisionSnapshotRenderer;
        this.verdictHeroRenderer = verdictHeroRenderer;
    }

    public AnalysisOverviewRenderer(MarkdownRenderer markdownRenderer,
                                    DecisionSnapshotRenderer decisionSnapshotRenderer) {
        this(markdownRenderer, decisionSnapshotRenderer, new VerdictHeroRenderer(markdownRenderer));
    }

    static Map<String, Double> normalizeFactorScores(Map<String, Double> factorScores) {
        if (factorScores == null || factorScores.isEmpty()) {
            return Map.of();
        }
        double maxScore = factorScores.values().stream()
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        double multiplier = calculateMultiplier(maxScore);
        Map<String, Double> normalized = new LinkedHashMap<>();
        factorScores.forEach((key, val) -> {
            double value = val != null ? val * multiplier : 0.0;
            normalized.put(key, Math.clamp(value, 0.0, 10.0));
        });
        return normalized;
    }

    private static double calculateMultiplier(double maxScore) {
        if (maxScore > 0.0 && maxScore <= 1.0) {
            return 10.0;
        }
        return maxScore > 10.0 ? 0.1 : 1.0;
    }

    public boolean hasValuationAndDriversContent(AnalysisResult snapshot) {
        if (snapshot == null) {
            return false;
        }
        boolean hasContext = snapshot.valuationOneLiner() != null && !snapshot.valuationOneLiner().isBlank();
        DecisionSummary s = snapshot.decisionSummary();
        boolean hasTension = s != null && s.keyValuationTension() != null && !s.keyValuationTension().isBlank();
        boolean hasDrivers = snapshot.decisionDrivers() != null && !snapshot.decisionDrivers().isEmpty();
        boolean hasAudit = snapshot.convictionReasons() != null && !snapshot.convictionReasons().isEmpty();
        return hasContext || hasTension || hasDrivers || hasAudit;
    }

    public Component renderOverviewTab(AnalysisResult snapshot, CompanyAnalysisDto analysis, Runnable onOpenDriversTab) {
        if (snapshot == null) {
            return null;
        }
        Div container = new Div();
        container.addClassNames(LumoUtility.Width.FULL);

        addIfNotNull(container, verdictHeroRenderer.renderHeroCard(snapshot, analysis));
        addPillarSignals(container, snapshot);
        addImpliedExpectations(container, snapshot.decisionSummary());
        addIfNotNull(container, createSnapshotPoints(snapshot, analysis));
        addIfNotNull(container, createOverviewWatchpointBar(snapshot, analysis, onOpenDriversTab));
        return container;
    }

    public Component renderValuationAndDriversTab(AnalysisResult snapshot, CompanyAnalysisDto analysis) {
        if (snapshot == null || decisionSnapshotRenderer == null) {
            return null;
        }
        return decisionSnapshotRenderer.renderValuationAndDriversTab(snapshot, analysis, markdownRenderer);
    }

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

    private void addPillarSignals(Div container, AnalysisResult snapshot) {
        DecisionSummary s = snapshot.decisionSummary();
        if (decisionSnapshotRenderer != null && s != null) {
            addIfNotNull(container, decisionSnapshotRenderer.renderUnifiedPillarsGrid(snapshot, s));
        } else if (snapshot.factorScores() != null && !snapshot.factorScores().isEmpty()) {
            container.add(createLegacyFactorScoresGrid(snapshot.factorScores()));
        }
    }

    private void addImpliedExpectations(Div container, DecisionSummary s) {
        if (decisionSnapshotRenderer != null && s != null) {
            addIfNotNull(container, decisionSnapshotRenderer.renderImpliedExpectationsCard(s));
        }
    }

    private Component createLegacyFactorScoresGrid(Map<String, Double> factorScores) {
        Div grid = new Div();
        grid.setWidthFull();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(200px, 1fr))")
                .set("gap", "12px")
                .set("margin-bottom", "14px");

        normalizeFactorScores(factorScores).forEach((factor, score) -> {
            String title = SnapshotUiSupport.formatDimensionLabel(factor);
            String badge = SnapshotUiSupport.resolveScoreBadgeLabel(score);
            String theme = SnapshotUiSupport.resolveScoreBadgeTheme(score);
            grid.add(SnapshotUiSupport.createScoreCard(title, score, badge, theme));
        });
        return grid;
    }

    private Component createSnapshotPoints(AnalysisResult snapshot, CompanyAnalysisDto analysis) {
        boolean hasBull = snapshot.topBullPoints() != null && !snapshot.topBullPoints().isEmpty();
        boolean hasBear = snapshot.topBearPoints() != null && !snapshot.topBearPoints().isEmpty();
        if (!hasBull && !hasBear) {
            return null;
        }
        Div pointsGrid = new Div();
        pointsGrid.setWidthFull();
        int cols = (hasBull && hasBear) ? 2 : 1;
        pointsGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(" + cols + ", minmax(0, 1fr))")
                .set("gap", "14px")
                .set("margin-bottom", "14px");

        if (hasBull) {
            pointsGrid.add(createPointsCard("Key Bull Drivers", snapshot.topBullPoints(), true, analysis));
        }
        if (hasBear) {
            pointsGrid.add(createPointsCard("Key Risks & Headwinds", snapshot.topBearPoints(), false, analysis));
        }
        return pointsGrid;
    }

    private Component createPointsCard(String titleText, List<String> items, boolean isBull, CompanyAnalysisDto analysis) {
        String bg = isBull ? "rgba(46, 204, 113, 0.05)" : "rgba(231, 76, 60, 0.05)";
        String borderColor = isBull ? "rgba(46, 204, 113, 0.2)" : "rgba(231, 76, 60, 0.2)";
        String headerColor = isBull ? SnapshotUiSupport.COLOR_GREEN : SnapshotUiSupport.COLOR_RED;

        Div card = SnapshotUiSupport.createTintedCard(bg, borderColor);
        card.getStyle().set("padding", "14px 16px");

        H5 header = new H5((isBull ? "▲ " : "▼ ") + titleText);
        header.getStyle()
                .set("margin", "0 0 10px 0")
                .set("color", headerColor)
                .set("font-size", "0.82rem")
                .set("font-weight", "700")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.05em");
        card.add(header, buildPointsList(items, analysis));
        return card;
    }

    private UnorderedList buildPointsList(List<String> items, CompanyAnalysisDto analysis) {
        UnorderedList list = new UnorderedList();
        list.getStyle()
                .set("margin", "0")
                .set("padding-left", "1.2rem")
                .set("font-size", "0.86rem")
                .set("line-height", "1.5");

        String jsonData = analysis != null ? analysis.getAnalysisData() : null;
        for (String item : items) {
            ListItem li = new ListItem(markdownRenderer.renderMarkdownWithCitations(item, jsonData));
            li.getStyle().set("margin-bottom", "5px");
            list.add(li);
        }
        return list;
    }

    private Component createOverviewWatchpointBar(AnalysisResult snapshot, CompanyAnalysisDto analysis, Runnable onOpenDriversTab) {
        boolean hasWatch = snapshot.whatWouldChangeThis() != null && !snapshot.whatWouldChangeThis().isBlank();
        boolean showDriversAction = onOpenDriversTab != null && hasValuationAndDriversContent(snapshot);
        if (!hasWatch && !showDriversAction) {
            return null;
        }

        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.getStyle()
                .set("background", "rgba(var(--lumo-contrast-rgb), 0.02)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "8px")
                .set("padding", "10px 16px")
                .set("gap", "16px")
                .set("flex-wrap", "wrap")
                .set("box-sizing", "border-box");

        if (hasWatch) {
            bar.add(buildWatchTriggerInline(snapshot.whatWouldChangeThis(), analysis));
        }
        if (showDriversAction) {
            bar.add(buildOpenDriversButton(onOpenDriversTab));
        }
        return bar;
    }

    private Div buildWatchTriggerInline(String watchText, CompanyAnalysisDto analysis) {
        Div left = new Div();
        left.getStyle().set("flex", "1 1 360px");

        Span titleSpan = new Span("Watch Trigger: ");
        titleSpan.getStyle().set("font-weight", "700").set("font-size", "0.82rem").set("color", SnapshotUiSupport.COLOR_AMBER);

        String jsonData = analysis != null ? analysis.getAnalysisData() : null;
        Component md = markdownRenderer.renderMarkdownWithCitations(watchText, jsonData);
        md.getStyle().set("display", "inline").set("font-size", "0.83rem").set("color", "var(--lumo-secondary-text-color)");

        left.add(titleSpan, md);
        return left;
    }

    private Button buildOpenDriversButton(Runnable onOpenDriversTab) {
        Button btn = new Button("Valuation & Drivers", VaadinIcon.ARROW_RIGHT.create(), _ -> onOpenDriversTab.run());
        btn.setIconAfterText(true);
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        return btn;
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

    private void addIfNotNull(Div parent, Component child) {
        if (child != null) {
            parent.add(child);
        }
    }
}
