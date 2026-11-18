package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AnalysisOverviewRenderer {

    private final MarkdownRenderer markdownRenderer;
    private final DecisionSnapshotRenderer decisionSnapshotRenderer;
    private final VerdictHeroRenderer verdictHeroRenderer;
    private final ScenarioSectionRenderer scenarioSectionRenderer;

    public static Map<String, Double> normalizeFactorScores(Map<String, Double> factorScores) {
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
        return scenarioSectionRenderer.renderScenariosTab(snapshot, analysis);
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
            String badge = SnapshotUiSupport.resolveScoreBadgeLabel(factor, score);
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

    private void addIfNotNull(Div parent, Component child) {
        if (child != null) {
            parent.add(child);
        }
    }
}
