package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionDriver;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.oraculum.analyst.api.dto.AnalysisResult.ImpliedEpsSnapshot;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class DecisionSnapshotRenderer {

    public Component renderDecisionSupportSection(AnalysisResult snapshot) {
        if (snapshot == null || (snapshot.decisionSummary() == null && snapshot.decisionDrivers() == null)) {
            return null;
        }
        VerticalLayout root = new VerticalLayout();
        root.setPadding(false);
        root.setSpacing(false);
        root.setWidthFull();
        root.getStyle().set("gap", "14px").set("margin-bottom", "16px");

        DecisionSummary summary = snapshot.decisionSummary();
        if (summary != null) {
            addIfPresent(root, renderPriceAndCatalystBar(summary));
            addIfPresent(root, renderFourDimensionGrid(summary));
            addIfPresent(root, renderDecisiveQuestionCard(summary));
            addIfPresent(root, renderImpliedExpectationsCard(summary));
        }
        addIfPresent(root, renderConvictionReasonsCard(snapshot.convictionReasons()));
        addIfPresent(root, renderDecisionDriversCard(snapshot.decisionDrivers()));
        return root;
    }

    private void addIfPresent(VerticalLayout parent, Component child) {
        if (child != null) {
            parent.add(child);
        }
    }

    private Component renderPriceAndCatalystBar(DecisionSummary s) {
        if (s.currentSharePrice() == null && s.priceDate() == null && s.nextCatalyst() == null) {
            return null;
        }
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.getStyle().set("gap", "10px").set("flex-wrap", "wrap");

        if (s.currentSharePrice() != null) {
            String priceText = String.format(Locale.US, "Ref. Price: $%.2f", s.currentSharePrice())
                    + (s.priceDate() != null ? " (as of " + s.priceDate() + ")" : "");
            bar.add(createPillBadge(priceText, "contrast"));
        }
        if (s.priceStalenessDays() != null && s.priceStalenessDays() > 2) {
            bar.add(createPillBadge("Price age: " + s.priceStalenessDays() + "d", "warning"));
        }
        if (s.nextCatalyst() != null) {
            bar.add(createPillBadge(s.nextCatalyst(), "primary"));
        }
        return bar;
    }

    private Component renderFourDimensionGrid(DecisionSummary s) {
        HorizontalLayout grid = new HorizontalLayout();
        grid.setWidthFull();
        grid.getStyle().set("gap", "12px").set("flex-wrap", "wrap");

        grid.add(
                createDimensionTile("Business Quality", s.businessQuality()),
                createDimensionTile("Valuation Hurdle", s.valuationVsExpectations()),
                createDimensionTile("Method Agreement", s.methodAgreement()),
                createDimensionTile("Market Setup", s.marketSetup())
        );
        return grid;
    }

    private Div createDimensionTile(String label, String rawValue) {
        Div tile = new Div();
        tile.getStyle()
                .set("flex", "1 1 180px")
                .set("background", "rgba(var(--lumo-contrast-rgb), 0.03)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "8px")
                .set("padding", "10px 14px");

        Span title = new Span(label);
        title.getStyle()
                .set("font-size", "0.75rem")
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("display", "block")
                .set("margin-bottom", "6px");

        String display = rawValue != null ? rawValue.replace('_', ' ') : "N/A";
        Span badge = createPillBadge(display, resolveDimensionTheme(rawValue));
        tile.add(title, badge);
        return tile;
    }

    private String resolveDimensionTheme(String value) {
        if (value == null) return "contrast";
        return switch (value) {
            case "EXCEPTIONAL", "STRONG", "LOW_HURDLE", "ALIGNED", "SUPPORTIVE_UPTREND", "SUPPORTIVE" -> "success";
            case "DEMANDING", "CONFLICTING_PEAK_MARGIN_RISK", "DIVERGENT", "PULLBACK_IN_UPTREND", "WARNING" -> "warning";
            case "SPECULATIVE", "WEAK", "DISTRIBUTION_WARNING", "DOWNTREND", "RISK" -> "error";
            default -> "contrast";
        };
    }

    private Component renderDecisiveQuestionCard(DecisionSummary s) {
        if (s.decisiveQuestion() == null && s.keyValuationTension() == null) {
            return null;
        }
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("background", "rgba(var(--lumo-warning-color-rgb), 0.06)")
                .set("border-left", "3px solid var(--lumo-warning-color)")
                .set("border-radius", "0 8px 8px 0")
                .set("padding", "10px 14px");

        if (s.decisiveQuestion() != null) {
            Span q = new Span("Decisive Question: " + s.decisiveQuestion());
            q.getStyle().set("font-weight", "600").set("display", "block").set("font-size", "0.9rem");
            card.add(q);
        }
        if (s.keyValuationTension() != null) {
            Span t = new Span(s.keyValuationTension());
            t.getStyle().set("font-size", "0.82rem").set("color", "var(--lumo-secondary-text-color)").set("display", "block").set("margin-top", "4px");
            card.add(t);
        }
        return card;
    }

    private Component renderImpliedExpectationsCard(DecisionSummary s) {
        boolean hasDcf = s.requiredFcfCagrPct() != null;
        boolean hasEpsMap = s.impliedEpsByMultiple() != null && !s.impliedEpsByMultiple().isEmpty();
        if (!hasDcf && !hasEpsMap) {
            return null;
        }
        Div card = createSectionBox("Price → Implied Expectations (What Today's Price Requires)");
        if (hasDcf) {
            card.add(buildDcfExpectationRow(s));
        }
        if (hasEpsMap) {
            card.add(buildImpliedEpsList(s.targetFiscalYear(), s.impliedEpsByMultiple()));
        }
        return card;
    }

    private Component buildDcfExpectationRow(DecisionSummary s) {
        String text = String.format(Locale.US,
                "Reverse DCF Hurdle: %.1f%% 10Y FCF CAGR required (vs. %s TTM FCF growth, %s historical CAGR)",
                s.requiredFcfCagrPct(),
                s.ttmFcfGrowthPct() != null ? String.format(Locale.US, "%.1f%%", s.ttmFcfGrowthPct()) : "N/A",
                s.historicalFcfCagrPct() != null ? String.format(Locale.US, "%.1f%%", s.historicalFcfCagrPct()) : "N/A");
        Span span = new Span(text);
        span.getStyle().set("font-size", "0.85rem").set("display", "block").set("margin-bottom", "8px");
        return span;
    }

    private Component buildImpliedEpsList(String targetFy, List<ImpliedEpsSnapshot> entries) {
        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);
        list.getStyle().set("gap", "6px");

        for (ImpliedEpsSnapshot e : entries) {
            String rowText = String.format(Locale.US,
                    "%s: Requires $%.2f EPS (%s) vs. Consensus $%.2f (%+.1f%% margin)",
                    e.anchorLabel() != null ? e.anchorLabel() : String.format(Locale.US, "%.1fx P/E", e.multiple()),
                    e.requiredEpsForCurrentPrice() != null ? e.requiredEpsForCurrentPrice() : 0.0f,
                    targetFy != null ? targetFy : "Target FY",
                    e.consensusEpsAvg() != null ? e.consensusEpsAvg() : 0.0f,
                    e.consensusVsRequiredPct() != null ? e.consensusVsRequiredPct() : 0.0f);
            Span item = new Span(rowText);
            item.getStyle().set("font-size", "0.82rem").set("color", "var(--lumo-body-text-color)");
            list.add(item);
        }
        return list;
    }

    private Component renderConvictionReasonsCard(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return null;
        }
        Div card = createSectionBox("Conviction Derivation & Guardrails");
        for (String reason : reasons) {
            Span item = new Span("• " + reason);
            item.getStyle().set("display", "block").set("font-size", "0.82rem").set("color", "var(--lumo-secondary-text-color)");
            card.add(item);
        }
        return card;
    }

    private Component renderDecisionDriversCard(List<DecisionDriver> drivers) {
        if (drivers == null || drivers.isEmpty()) {
            return null;
        }
        Div card = createSectionBox("Key Decision Drivers & Thesis Triggers");
        VerticalLayout rows = new VerticalLayout();
        rows.setPadding(false);
        rows.setSpacing(false);
        rows.getStyle().set("gap", "8px");

        for (DecisionDriver d : drivers) {
            rows.add(buildDriverRow(d));
        }
        card.add(rows);
        return card;
    }

    private Component buildDriverRow(DecisionDriver d) {
        Div row = new Div();
        row.setWidthFull();
        row.getStyle()
                .set("padding", "8px 10px")
                .set("border-radius", "6px")
                .set("background", "rgba(var(--lumo-contrast-rgb), 0.02)")
                .set("border", "1px solid var(--lumo-contrast-5pct)");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        Span factor = new Span(d.factor());
        factor.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        factor.getStyle().set("font-size", "0.85rem");
        header.add(factor, createPillBadge(d.status(), resolveDimensionTheme(d.status())));

        Span details = new Span(String.format("Current: %s  |  Benchmark: %s  |  Trigger: %s",
                d.currentValue(), d.benchmarkOrRequired(), d.whatChangesView()));
        details.getStyle().set("display", "block").set("font-size", "0.78rem").set("color", "var(--lumo-secondary-text-color)").set("margin-top", "4px");

        row.add(header, details);
        return row;
    }

    private Div createSectionBox(String titleText) {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("background", "rgba(var(--lumo-contrast-rgb), 0.03)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "8px")
                .set("padding", "12px 16px");
        H4 title = new H4(titleText);
        title.getStyle().set("margin", "0 0 8px 0").set("font-size", "0.9rem");
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
