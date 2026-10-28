package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionDriver;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.oraculum.analyst.api.dto.AnalysisResult.ImpliedEpsSnapshot;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DecisionSnapshotRenderer {

    public Component renderDecisionSupportSection(AnalysisResult snapshot) {
        if (snapshot == null || snapshot.decisionSummary() == null) {
            return null;
        }
        DecisionSummary s = snapshot.decisionSummary();
        VerticalLayout root = new VerticalLayout();
        root.setPadding(false);
        root.setSpacing(true);
        root.setWidthFull();
        root.getStyle().set("margin-bottom", "16px").set("gap", "14px");

        addIfPresent(root, renderPriceAndCatalystBar(s));
        addIfPresent(root, renderDecisiveQuestionCard(s));
        addIfPresent(root, renderUnifiedPillarsGrid(snapshot, s));
        addIfPresent(root, renderImpliedExpectationsCard(s));
        addIfPresent(root, renderDecisionDriversCard(snapshot.decisionDrivers()));
        addIfPresent(root, renderConvictionReasonsCard(snapshot.convictionReasons()));
        return root;
    }

    private void addIfPresent(VerticalLayout parent, Component child) {
        if (child != null) {
            parent.add(child);
        }
    }

    public Component renderPriceAndCatalystBar(DecisionSummary s) {
        if (s == null || (s.currentSharePrice() == null && s.priceDate() == null && s.nextCatalyst() == null)) {
            return null;
        }
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.getStyle().set("gap", "10px").set("flex-wrap", "wrap").set("margin-bottom", "16px");

        if (s.currentSharePrice() != null) {
            String priceText = String.format(Locale.US, "Ref. Share Price: $%.2f", s.currentSharePrice())
                    + (s.priceDate() != null ? " (as of " + s.priceDate() + ")" : "");
            bar.add(createPillBadge(priceText, "contrast"));
        }
        if (s.nextCatalyst() != null) {
            bar.add(createPillBadge("🎯 " + s.nextCatalyst(), "primary"));
        }
        return bar;
    }

    public Component renderUnifiedPillarsGrid(AnalysisResult snapshot, DecisionSummary s) {
        HorizontalLayout grid = new HorizontalLayout();
        grid.setWidthFull();
        grid.getStyle().set("gap", "12px").set("flex-wrap", "wrap").set("margin-bottom", "16px");

        Map<String, Double> scores = snapshot.factorScores() != null ? snapshot.factorScores() : Map.of();
        double fundScore = scores.getOrDefault("fundamental_health", 8.5);
        double valScore = scores.getOrDefault("valuation", 6.0);
        double growthScore = scores.getOrDefault("growth_prospects", 8.0);
        double riskScore = scores.getOrDefault("risk_profile", 7.0);

        String growthLabel = resolveGrowthLabel(growthScore, s);
        String growthTheme = resolveGrowthTheme(growthScore, s);

        grid.add(
                buildPillarTile("Fundamental Health", fundScore, s.businessQuality(), resolveDimensionTheme(s.businessQuality())),
                buildPillarTile("Valuation Hurdle", valScore, s.valuationVsExpectations(), resolveDimensionTheme(s.valuationVsExpectations())),
                buildPillarTile("Growth Prospects", growthScore, growthLabel, growthTheme),
                buildPillarTile("Risk & Market Setup", riskScore, s.marketSetup(), resolveDimensionTheme(s.marketSetup()))
        );
        return grid;
    }

    private String resolveGrowthLabel(double score, DecisionSummary s) {
        if (s != null && s.ttmFcfGrowthPct() != null && s.ttmFcfGrowthPct() > 30.0) {
            return "HIGH EXPANSION";
        }
        if (score >= 7.5) return "STRONG MOMENTUM";
        if (score >= 5.5) return "STEADY GROWTH";
        if (score >= 4.0) return "MODERATE SLOWDOWN";
        return "CONTRACTION";
    }

    @SuppressWarnings("unused")
    private String resolveGrowthTheme(double score, DecisionSummary s) {
        if (score >= 7.0) return "primary";
        if (score >= 5.0) return "contrast";
        return "warning";
    }

    private Div buildPillarTile(String title, double score, String badgeText, String theme) {
        Div tile = new Div();
        tile.getStyle()
                .set("flex", "1 1 200px")
                .set("background", "rgba(var(--lumo-contrast-rgb), 0.03)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "8px")
                .set("padding", "12px 14px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "space-between")
                .set("gap", "10px");

        Span titleSpan = createPillarHeader(title);
        HorizontalLayout meter = renderScoreMeter(score);
        String label = badgeText != null ? badgeText.replace('_', ' ') : "N/A";
        Span badge = createPillBadge(label, theme);
        badge.getStyle().set("align-self", "flex-start");

        tile.add(titleSpan, meter, badge);
        return tile;
    }

    private Span createPillarHeader(String title) {
        Span titleSpan = new Span(title.toUpperCase(Locale.ROOT));
        titleSpan.getStyle()
                .set("font-size", "0.75rem")
                .set("font-weight", "700")
                .set("letter-spacing", "0.5px")
                .set("color", "var(--lumo-secondary-text-color)");
        return titleSpan;
    }

    private HorizontalLayout renderScoreMeter(double score) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.getStyle().set("gap", "8px");

        HorizontalLayout bars = buildBars(score);
        Span scoreText = new Span(String.format(Locale.US, "%.1f", score));
        scoreText.getStyle()
                .set("font-size", "0.85rem")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-header-text-color)");

        layout.add(bars, scoreText);
        return layout;
    }

    private HorizontalLayout buildBars(double score) {
        HorizontalLayout bars = new HorizontalLayout();
        bars.setSpacing(false);
        bars.getStyle().set("gap", "3px").set("align-items", "center");

        int fullBlocks = (int) Math.round(score);
        for (int i = 1; i <= 10; i++) {
            bars.add(createBarSegment(i, fullBlocks));
        }
        return bars;
    }

    private Span createBarSegment(int index, int fullBlocks) {
        Span bar = new Span();
        bar.getStyle()
                .set("width", "8px")
                .set("height", "13px")
                .set("border-radius", "2px");

        if (index <= fullBlocks) {
            String col = (index <= 3) ? "var(--lumo-error-color)"
                    : (index <= 7) ? "var(--lumo-warning-color)" : "var(--lumo-success-color)";
            bar.getStyle().set("background", col);
        } else {
            bar.getStyle().set("background", "rgba(255, 255, 255, 0.12)");
        }
        return bar;
    }

    private String resolveDimensionTheme(String value) {
        if (value == null) return "contrast";
        String norm = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (norm) {
            case "EXCEPTIONAL", "HIGH", "STRONG", "LOW_HURDLE", "CONSERVATIVE", "ALIGNED", "SUPPORTIVE_UPTREND",
                 "SUPPORTIVE" -> "success";
            case "DEMANDING", "AGGRESSIVE", "CONFLICTING_PEAK_MARGIN_RISK", "DIVERGENT", "PULLBACK_IN_UPTREND",
                 "WARNING" -> "warning";
            case "SPECULATIVE", "WEAK", "VULNERABLE", "DISTRIBUTION_WARNING", "DOWNTREND", "RISK" -> "error";
            default -> "contrast";
        };
    }

    public Component renderDecisiveQuestionCard(DecisionSummary s) {
        if (s == null || (s.decisiveQuestion() == null && s.keyValuationTension() == null)) {
            return null;
        }
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("background", "rgba(241, 196, 15, 0.04)")
                .set("border", "1px solid rgba(241, 196, 15, 0.2)")
                .set("border-left", "4px solid var(--lumo-warning-color)")
                .set("border-radius", "8px")
                .set("padding", "14px 16px")
                .set("margin-bottom", "14px");

        card.add(buildQuestionHeader(s));
        if (s.decisiveQuestion() != null) {
            card.add(buildQuestionText(s.decisiveQuestion()));
        }
        if (s.keyValuationTension() != null) {
            card.add(buildTensionText(s.keyValuationTension()));
        }
        return card;
    }

    private HorizontalLayout buildQuestionHeader(DecisionSummary s) {
        HorizontalLayout topRow = new HorizontalLayout();
        topRow.setWidthFull();
        topRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topRow.setAlignItems(FlexComponent.Alignment.CENTER);

        Span header = new Span("💡 DECISIVE QUESTION");
        header.getStyle()
                .set("font-size", "0.82rem")
                .set("font-weight", "700")
                .set("letter-spacing", "0.5px")
                .set("color", "var(--lumo-warning-text-color)");
        topRow.add(header);

        if (s.methodAgreement() != null) {
            String label = "⚖️ METHOD CONFLICT: " + s.methodAgreement().replace('_', ' ');
            topRow.add(createPillBadge(label, resolveDimensionTheme(s.methodAgreement())));
        }
        return topRow;
    }

    private Span buildQuestionText(String question) {
        Span q = new Span(question);
        q.getStyle()
                .set("font-weight", "600")
                .set("display", "block")
                .set("font-size", "0.95rem")
                .set("line-height", "1.5")
                .set("margin-top", "8px")
                .set("color", "var(--lumo-header-text-color)");
        return q;
    }

    private Span buildTensionText(String tension) {
        Span t = new Span(tension);
        t.getStyle()
                .set("font-size", "0.82rem")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("display", "block")
                .set("line-height", "1.4")
                .set("margin-top", "4px");
        return t;
    }

    public Component renderImpliedExpectationsCard(DecisionSummary s) {
        if (s == null) return null;
        boolean hasDcf = s.requiredFcfCagrPct() != null;
        boolean hasEpsMap = s.impliedEpsByMultiple() != null && !s.impliedEpsByMultiple().isEmpty();
        if (!hasDcf && !hasEpsMap) {
            return null;
        }

        Div card = createSectionBox("📊 PRICE → IMPLIED EXPECTATIONS (WHAT TODAY'S PRICE REQUIRES)");
        HorizontalLayout columns = new HorizontalLayout();
        columns.setWidthFull();
        columns.getStyle().set("gap", "14px").set("flex-wrap", "wrap").set("margin-top", "10px");

        if (hasDcf) {
            columns.add(buildDcfColumn(s));
        }
        if (hasEpsMap) {
            columns.add(buildPeMapColumn(s.targetFiscalYear(), s.impliedEpsByMultiple()));
        }
        card.add(columns);
        return card;
    }

    private Component buildDcfColumn(DecisionSummary s) {
        Div box = new Div();
        box.getStyle()
                .set("flex", "1 1 300px")
                .set("background", "rgba(59, 130, 246, 0.04)")
                .set("border", "1px solid rgba(59, 130, 246, 0.2)")
                .set("border-radius", "8px")
                .set("padding", "14px 16px");

        H5 title = new H5("📈 10-YEAR REVERSE DCF HURDLE");
        title.getStyle()
                .set("margin", "0 0 10px 0")
                .set("color", "#3b82f6")
                .set("font-size", "0.85rem")
                .set("font-weight", "700")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px");

        HorizontalLayout highlight = new HorizontalLayout();
        highlight.setAlignItems(FlexComponent.Alignment.BASELINE);
        highlight.getStyle().set("gap", "8px").set("margin-bottom", "10px");

        Span cagr = new Span(String.format(Locale.US, "%.1f%%", s.requiredFcfCagrPct()));
        cagr.getStyle().set("font-size", "1.6rem").set("font-weight", "800").set("color", "var(--lumo-warning-color)");
        Span unit = new Span("10Y FCF CAGR required");
        unit.getStyle().set("font-size", "0.85rem").set("color", "var(--lumo-secondary-text-color)");
        highlight.add(cagr, unit);

        VerticalLayout stats = new VerticalLayout();
        stats.setPadding(false);
        stats.setSpacing(false);
        stats.getStyle().set("gap", "6px").set("font-size", "0.85rem");

        if (s.ttmFcfGrowthPct() != null) {
            stats.add(buildStatLine("Recent TTM FCF Growth", String.format(Locale.US, "%.1f%%", s.ttmFcfGrowthPct())));
        }
        if (s.historicalFcfCagrPct() != null) {
            stats.add(buildStatLine("5Y Historical FCF CAGR", String.format(Locale.US, "%.1f%%", s.historicalFcfCagrPct())));
        }
        box.add(title, highlight, stats);
        return box;
    }

    private Component buildStatLine(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        Span l = new Span(label + ":");
        l.getStyle().set("color", "var(--lumo-secondary-text-color)");
        Span v = new Span(value);
        v.getStyle().set("font-weight", "600");
        row.add(l, v);
        return row;
    }

    private Component buildPeMapColumn(String targetFy, List<ImpliedEpsSnapshot> entries) {
        Div box = new Div();
        box.getStyle()
                .set("flex", "1.3 1 360px")
                .set("background", "rgba(168, 85, 247, 0.04)")
                .set("border", "1px solid rgba(168, 85, 247, 0.2)")
                .set("border-radius", "8px")
                .set("padding", "14px 16px");

        String fyLabel = (targetFy != null ? targetFy : "FY28");
        H5 title = new H5("🎯 REVERSE P/E MAP (TARGET " + fyLabel + " EPS)");
        title.getStyle()
                .set("margin", "0 0 10px 0")
                .set("color", "#a855f7")
                .set("font-size", "0.85rem")
                .set("font-weight", "700")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px");

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);
        list.getStyle().set("gap", "8px");

        for (ImpliedEpsSnapshot e : entries) {
            list.add(buildPeMapRow(e));
        }
        box.add(title, list);
        return box;
    }

    private Component buildPeMapRow(ImpliedEpsSnapshot e) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.getStyle().set("font-size", "0.84rem");

        String label = e.anchorLabel() != null ? e.anchorLabel() : String.format(Locale.US, "%.1fx", e.multiple());
        Span left = new Span(label);
        left.getStyle().set("font-weight", "600");

        HorizontalLayout right = new HorizontalLayout();
        right.setAlignItems(FlexComponent.Alignment.CENTER);
        right.getStyle().set("gap", "8px");

        Span req = new Span(String.format(Locale.US, "Requires $%.2f", e.requiredEpsForCurrentPrice() != null ? e.requiredEpsForCurrentPrice() : 0.0f));
        req.getStyle().set("color", "var(--lumo-secondary-text-color)");

        float margin = e.consensusVsRequiredPct() != null ? e.consensusVsRequiredPct() : 0.0f;
        String theme = margin >= 20.0f ? "success" : (margin >= 0.0f ? "contrast" : "warning");
        Span badge = createPillBadge(String.format(Locale.US, "%+.1f%% buffer", margin), theme);

        right.add(req, badge);
        row.add(left, right);
        return row;
    }

    public Component renderDecisionDriversCard(List<DecisionDriver> drivers) {
        if (drivers == null || drivers.isEmpty()) {
            return null;
        }
        Div card = createSectionBox("🚦 KEY DECISION DRIVERS & THESIS TRIGGERS");
        HorizontalLayout grid = new HorizontalLayout();
        grid.setWidthFull();
        grid.getStyle().set("gap", "12px").set("flex-wrap", "wrap").set("margin-top", "10px");

        for (DecisionDriver d : drivers) {
            grid.add(buildDriverTile(d));
        }
        card.add(grid);
        return card;
    }

    private Component buildDriverTile(DecisionDriver d) {
        Div tile = new Div();
        tile.getStyle()
                .set("flex", "1 1 320px")
                .set("padding", "12px 14px")
                .set("border-radius", "8px")
                .set("background", resolveDriverBackground(d.status()))
                .set("border", "1px solid " + resolveDriverBorderColor(d.status(), 0.2))
                .set("border-left", "4px solid " + resolveDriverBorderColor(d.status(), 1.0))
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "space-between")
                .set("gap", "8px");

        HorizontalLayout top = new HorizontalLayout();
        top.setWidthFull();
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        top.setAlignItems(FlexComponent.Alignment.CENTER);

        Span factor = new Span(d.factor());
        factor.addClassName(LumoUtility.FontWeight.BOLD);
        factor.getStyle().set("font-size", "0.85rem").set("color", "var(--lumo-header-text-color)");
        top.add(factor, createPillBadge(d.status(), resolveDimensionTheme(d.status())));

        Div details = new Div();
        details.getStyle().set("font-size", "0.80rem");

        Span curSpan = new Span("Current: " + d.currentValue() + "  |  Hurdle: " + d.benchmarkOrRequired());
        curSpan.getStyle().set("display", "block").set("color", "var(--lumo-body-text-color)");

        Span trigSpan = new Span("⚡ Trigger: " + d.whatChangesView());
        trigSpan.getStyle().set("display", "block").set("color", "var(--lumo-secondary-text-color)").set("margin-top", "4px");

        details.add(curSpan, trigSpan);
        tile.add(top, details);
        return tile;
    }

    private String resolveDriverBackground(String status) {
        if (status == null) return "rgba(var(--lumo-contrast-rgb), 0.02)";
        return switch (status) {
            case "SUPPORTIVE" -> "rgba(46, 204, 113, 0.04)";
            case "DEMANDING" -> "rgba(230, 126, 34, 0.04)";
            case "WARNING" -> "rgba(241, 196, 15, 0.04)";
            case "RISK" -> "rgba(231, 76, 60, 0.04)";
            default -> "rgba(var(--lumo-contrast-rgb), 0.02)";
        };
    }

    private String resolveDriverBorderColor(String status, double opacity) {
        if (opacity < 1.0) {
            return switch (status != null ? status : "") {
                case "SUPPORTIVE" -> "rgba(46, 204, 113, 0.2)";
                case "DEMANDING" -> "rgba(230, 126, 34, 0.2)";
                case "WARNING" -> "rgba(241, 196, 15, 0.2)";
                case "RISK" -> "rgba(231, 76, 60, 0.2)";
                default -> "var(--lumo-contrast-10pct)";
            };
        }
        return switch (status != null ? status : "") {
            case "SUPPORTIVE" -> "#2ecc71";
            case "DEMANDING" -> "#e67e22";
            case "WARNING" -> "#f1c40f";
            case "RISK" -> "#e74c3c";
            default -> "var(--lumo-contrast-20pct)";
        };
    }

    public Component renderConvictionReasonsCard(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return null;
        }
        Div card = createSectionBox("🛡️ CONVICTION AUDIT & ACTIVE GUARDRAILS");
        card.getStyle()
                .set("background", "rgba(var(--lumo-contrast-rgb), 0.03)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "8px")
                .set("padding", "12px 16px")
                .set("margin-top", "14px");

        HorizontalLayout list = new HorizontalLayout();
        list.setWidthFull();
        list.getStyle().set("gap", "10px").set("flex-wrap", "wrap").set("margin-top", "8px");

        for (String reason : reasons) {
            list.add(buildAuditChip(reason));
        }
        card.add(list);
        return card;
    }

    private Component buildAuditChip(String reason) {
        Div chip = new Div();
        chip.getStyle()
                .set("flex", "1 1 280px")
                .set("padding", "8px 12px")
                .set("border-radius", "6px")
                .set("background", "rgba(var(--lumo-contrast-rgb), 0.02)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("font-size", "0.80rem")
                .set("color", "var(--lumo-secondary-text-color)");

        String cleaned = reason.replaceFirst("^Capped at \\d/5:\\s*", "🔒 Cap: ");
        if (cleaned.startsWith("Base conviction")) {
            chip.getStyle().set("border-left", "3px solid var(--lumo-primary-color)");
            chip.setText("📊 " + cleaned);
        } else {
            chip.getStyle().set("border-left", "3px solid var(--lumo-warning-color)");
            chip.setText(cleaned);
        }
        return chip;
    }

    private Div createSectionBox(String titleText) {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("background", "rgba(var(--lumo-contrast-rgb), 0.03)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "8px")
                .set("padding", "14px 16px");

        H5 title = new H5(titleText);
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "0.85rem")
                .set("font-weight", "700")
                .set("letter-spacing", "0.5px")
                .set("text-transform", "uppercase")
                .set("color", "var(--lumo-header-text-color)");
        card.add(title);
        return card;
    }

    private Span createPillBadge(String text, String theme) {
        Span badge = new Span(text != null ? text : "-");
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add("pill");
        if (theme != null && !"contrast".equals(theme)) {
            badge.getElement().getThemeList().add(theme);
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        badge.getStyle().set("font-size", "0.75rem").set("padding", "2px 8px");
        return badge;
    }
}
