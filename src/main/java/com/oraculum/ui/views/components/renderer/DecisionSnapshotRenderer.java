package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionDriver;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.oraculum.analyst.api.dto.AnalysisResult.ImpliedEpsSnapshot;
import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders compact decision-support blocks for the Overview tab (4 pillars and
 * unified implied-expectations scale) and delegates the Valuation &amp; Drivers tab.
 */
@Service
public class DecisionSnapshotRenderer {

    private final ValuationDriversRenderer valuationDriversRenderer = new ValuationDriversRenderer();

    public Component renderDecisionSupportSection(AnalysisResult snapshot) {
        if (snapshot == null || snapshot.decisionSummary() == null) {
            return null;
        }
        DecisionSummary s = snapshot.decisionSummary();
        VerticalLayout root = new VerticalLayout();
        root.setPadding(false);
        root.setSpacing(false);
        root.setWidthFull();
        root.getStyle().set("gap", "14px");

        addIfNotNull(root, renderUnifiedPillarsGrid(snapshot, s));
        addIfNotNull(root, renderImpliedExpectationsCard(s));
        addIfNotNull(root, renderDecisionDriversGrid(snapshot.decisionDrivers()));
        addIfNotNull(root, renderConvictionAuditCard(snapshot.conviction(), snapshot.convictionReasons()));
        return root;
    }

    public Component renderUnifiedPillarsGrid(AnalysisResult snapshot, DecisionSummary s) {
        if (snapshot == null || s == null) {
            return null;
        }
        Div grid = new Div();
        grid.setWidthFull();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(200px, 1fr))")
                .set("gap", "12px")
                .set("margin-bottom", "14px");

        Map<String, Double> scores = AnalysisOverviewRenderer.normalizeFactorScores(snapshot.factorScores());
        double fundScore = scores.getOrDefault("fundamental_health", 0.0);
        double valScore = scores.getOrDefault("valuation", 0.0);
        double growthScore = scores.getOrDefault("growth_prospects", 0.0);
        double riskScore = scores.getOrDefault("risk_profile", 0.0);

        String riskBadge = resolveRiskAndSetupLabel(riskScore, s.marketSetup());
        grid.add(
                SnapshotUiSupport.createScoreCard("Fundamental Health", fundScore, s.businessQuality(), SnapshotUiSupport.resolveDimensionTheme(s.businessQuality())),
                SnapshotUiSupport.createScoreCard("Valuation Attractiveness", valScore, s.valuationVsExpectations(), SnapshotUiSupport.resolveDimensionTheme(s.valuationVsExpectations())),
                SnapshotUiSupport.createScoreCard("Growth Prospects", growthScore, resolveGrowthLabel(growthScore, s), resolveGrowthTheme(growthScore)),
                SnapshotUiSupport.createScoreCard("Risk Resilience & Setup", riskScore, riskBadge, SnapshotUiSupport.resolveScoreBadgeTheme(riskScore))
        );
        return grid;
    }

    public static String resolveRiskAndSetupLabel(double riskScore, String marketSetup) {
        String riskTier = riskScore >= 7.0 ? "Low Fin. Risk" : (riskScore >= 5.0 ? "Moderate Risk" : "High Risk");
        if (marketSetup == null || marketSetup.isBlank()) {
            return riskTier;
        }
        return riskTier + " · " + SnapshotUiSupport.formatDimensionLabel(marketSetup);
    }

    public static String resolveGrowthLabel(double score, DecisionSummary s) {
        if (s != null && s.ttmFcfGrowthPct() != null && s.ttmFcfGrowthPct() > 30.0f) {
            return "High Expansion";
        }
        if (score >= 7.5) return "Strong Momentum";
        if (score >= 5.5) return "Steady Growth";
        return score >= 4.0 ? "Moderate" : "Contraction";
    }

    public static String resolveGrowthTheme(double score) {
        if (score >= 7.0) return "success";
        return score >= 5.0 ? "contrast" : "warning";
    }

    public Component renderImpliedExpectationsCard(DecisionSummary s) {
        if (s == null) {
            return null;
        }
        boolean hasDcf = s.requiredFcfCagrPct() != null;
        boolean hasEps = s.impliedEpsByMultiple() != null && !s.impliedEpsByMultiple().isEmpty();
        if (!hasDcf && !hasEps) {
            return null;
        }

        Div card = SnapshotUiSupport.createNeutralCard();
        card.getStyle().set("margin-bottom", "14px");
        card.add(buildExpectationsHeader(s), buildExpectationsBody(s, hasDcf, hasEps));
        return card;
    }

    private HorizontalLayout buildExpectationsHeader(DecisionSummary s) {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        Span title = new Span("WHAT TODAY'S PRICE REQUIRES");
        title.getStyle()
                .set("font-size", "0.73rem")
                .set("font-weight", "700")
                .set("letter-spacing", "0.06em")
                .set("color", "var(--lumo-secondary-text-color)");
        header.add(title);

        if (s.methodAgreement() != null && !s.methodAgreement().equalsIgnoreCase("ALIGNED")) {
            String label = "Method Conflict: " + SnapshotUiSupport.formatDimensionLabel(s.methodAgreement());
            header.add(SnapshotUiSupport.createStatusBadge(label, "warning"));
        }
        return header;
    }

    private Div buildExpectationsBody(DecisionSummary s, boolean hasDcf, boolean hasEps) {
        Div body = new Div();
        body.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "24px")
                .set("align-items", "center")
                .set("margin-top", "10px");

        if (hasDcf) {
            body.add(buildCompactDcfBlock(s, hasEps));
        }
        if (hasEps) {
            body.add(buildHorizontalPeScale(s.targetFiscalYear(), s.impliedEpsByMultiple()));
        }
        return body;
    }

    private Div buildCompactDcfBlock(DecisionSummary s, boolean showRightBorder) {
        Div dcf = new Div();
        dcf.getStyle().set("flex", "1 1 230px");
        if (showRightBorder) {
            dcf.getStyle().set("padding-right", "20px").set("border-right", "1px solid var(--lumo-contrast-10pct)");
        }

        Span lbl = new Span("10-Year Reverse DCF Hurdle");
        lbl.getStyle().set("display", "block").set("font-size", "0.76rem").set("color", "var(--lumo-secondary-text-color)");

        Span history = new Span(formatHistoricalFcfBenchmarks(s));
        history.getStyle().set("display", "block").set("font-size", "0.75rem").set("color", "var(--lumo-secondary-text-color)");

        dcf.add(lbl, buildDcfValueRow(s.requiredFcfCagrPct()), history);
        return dcf;
    }

    private HorizontalLayout buildDcfValueRow(Float requiredFcfCagrPct) {
        HorizontalLayout valRow = new HorizontalLayout();
        valRow.setAlignItems(FlexComponent.Alignment.BASELINE);
        valRow.getStyle().set("gap", "8px").set("margin", "4px 0");

        Span cagr = new Span(String.format(Locale.US, "%.1f%%", requiredFcfCagrPct));
        cagr.getStyle().set("font-size", "1.45rem").set("font-weight", "800").set("color", "var(--lumo-header-text-color)");
        Span unit = new Span("FCF CAGR");
        unit.getStyle().set("font-size", "0.80rem").set("color", "var(--lumo-secondary-text-color)");
        valRow.add(cagr, unit);
        return valRow;
    }

    private String formatHistoricalFcfBenchmarks(DecisionSummary s) {
        String ttm = s.ttmFcfGrowthPct() != null ? String.format(Locale.US, "Recent TTM: %.1f%%", s.ttmFcfGrowthPct()) : "";
        String fiveY = s.historicalFcfCagrPct() != null ? String.format(Locale.US, "5Y CAGR: %.1f%%", s.historicalFcfCagrPct()) : "";
        if (!ttm.isEmpty() && !fiveY.isEmpty()) return ttm + "  •  " + fiveY;
        return !ttm.isEmpty() ? ttm : fiveY;
    }

    private Div buildHorizontalPeScale(String targetFy, List<ImpliedEpsSnapshot> entries) {
        Div wrap = new Div();
        wrap.getStyle().set("flex", "2 1 460px");
        wrap.add(buildPeScaleTopBar(targetFy, entries));

        Div scaleGrid = new Div();
        scaleGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(140px, 1fr))")
                .set("gap", "12px")
                .set("margin-top", "6px");

        for (ImpliedEpsSnapshot entry : entries) {
            scaleGrid.add(buildPeScaleNode(entry));
        }
        wrap.add(scaleGrid);
        return wrap;
    }

    private HorizontalLayout buildPeScaleTopBar(String targetFy, List<ImpliedEpsSnapshot> entries) {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        bar.getStyle().set("font-size", "0.76rem").set("color", "var(--lumo-secondary-text-color)");

        String fy = targetFy != null ? targetFy : "Target FY";
        bar.add(new Span("Required EPS by Multiple (" + fy + ")"));

        Float consEps = entries.getFirst().consensusEpsAvg();
        if (consEps != null) {
            Span cons = new Span(String.format(Locale.US, "Consensus EPS: $%.2f", consEps));
            cons.getStyle().set("font-weight", "600").set("color", "var(--lumo-header-text-color)");
            bar.add(cons);
        }
        return bar;
    }

    private Div buildPeScaleNode(ImpliedEpsSnapshot e) {
        Div node = new Div();
        node.getStyle()
                .set("border-top", "2px solid var(--lumo-contrast-20pct)")
                .set("padding-top", "8px");

        Span mult = new Span(formatShortAnchorLabel(e));
        mult.getStyle().set("display", "block").set("font-size", "0.80rem").set("font-weight", "700").set("color", "var(--lumo-header-text-color)");

        float reqEps = e.requiredEpsForCurrentPrice() != null ? e.requiredEpsForCurrentPrice() : 0.0f;
        Span req = new Span(String.format(Locale.US, "Requires $%.2f EPS", reqEps));
        req.getStyle().set("display", "block").set("font-size", "0.78rem").set("color", "var(--lumo-body-text-color)").set("margin-top", "2px");

        node.add(mult, req, buildBufferDeltaSpan(e.consensusVsRequiredPct()));
        return node;
    }

    public static String formatShortAnchorLabel(ImpliedEpsSnapshot e) {
        String multStr = e.multiple() != null ? String.format(Locale.US, "%.1fx", e.multiple()) : "";
        if (e.anchorLabel() == null || e.anchorLabel().isBlank()) return multStr;
        String stripped = e.anchorLabel().replaceAll("\\s*\\([^)]*\\)", "").trim();
        return stripped.isEmpty() ? multStr : multStr + " • " + stripped;
    }

    private Span buildBufferDeltaSpan(Float bufferPct) {
        float val = bufferPct != null ? bufferPct : 0.0f;
        Span delta = new Span(String.format(Locale.US, "%+.1f%% buffer", val));
        String color = val >= 15.0f ? SnapshotUiSupport.COLOR_GREEN
                : (val >= 0.0f ? "var(--lumo-secondary-text-color)" : SnapshotUiSupport.COLOR_RED);
        delta.getStyle()
                .set("display", "block")
                .set("font-size", "0.75rem")
                .set("font-weight", "600")
                .set("color", color)
                .set("margin-top", "2px");
        return delta;
    }

    public Component renderValuationAndDriversTab(AnalysisResult snapshot, CompanyAnalysisDto analysis, MarkdownRenderer markdownRenderer) {
        return valuationDriversRenderer.renderValuationAndDriversTab(snapshot, analysis, markdownRenderer);
    }

    public Component renderDecisionDriversGrid(List<DecisionDriver> drivers) {
        return valuationDriversRenderer.renderDecisionDriversGrid(drivers);
    }

    public Component renderConvictionAuditCard(int conviction, List<String> reasons) {
        return valuationDriversRenderer.renderConvictionAuditCard(conviction, reasons);
    }

    private void addIfNotNull(VerticalLayout parent, Component child) {
        if (child != null) {
            parent.add(child);
        }
    }
}
