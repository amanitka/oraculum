package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.ValuationAssessment;
import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Renders the top-level Investment Verdict Hero card on the Overview tab,
 * visually separating business trajectory from stock valuation and surfacing
 * the core investment stance, thesis, and decisive question at a glance.
 */
@Service
@RequiredArgsConstructor
public class VerdictHeroRenderer {

    private final MarkdownRenderer markdownRenderer;

    record VerdictStance(String label, String summaryWhy, String accentColor, String bgTint, String borderTint) {
    }

    public Component renderHeroCard(AnalysisResult snapshot, CompanyAnalysisDto analysis) {
        if (snapshot == null) {
            return null;
        }
        VerdictStance stance = deriveStance(snapshot.valuation(), snapshot.outlook(), snapshot.conviction());
        Div hero = createHeroContainer(stance);
        hero.add(buildTopHeaderRow(snapshot, stance));
        addThesisSection(hero, snapshot, analysis);
        addDecisiveQuestionStrip(hero, snapshot.decisionSummary());
        return hero;
    }

    static VerdictStance deriveStance(ValuationAssessment valuation, AnalysisOutlook outlook, int conviction) {
        if (valuation == ValuationAssessment.UNDERVALUED) {
            return deriveUndervaluedStance(outlook, conviction);
        }
        if (valuation == ValuationAssessment.FAIRLY_VALUED) {
            return deriveFairlyValuedStance(outlook);
        }
        if (valuation == ValuationAssessment.OVERVALUED) {
            return deriveOvervaluedStance(outlook);
        }
        return new VerdictStance("MONITOR / INCONCLUSIVE",
                "Conflicting valuation signals or limited visibility for a directional stance.",
                SnapshotUiSupport.COLOR_SLATE, "rgba(149, 165, 166, 0.12)", "rgba(149, 165, 166, 0.35)");
    }

    private static VerdictStance deriveUndervaluedStance(AnalysisOutlook outlook, int conviction) {
        if (outlook == AnalysisOutlook.BEARISH) {
            return new VerdictStance("WATCHLIST • VALUE TRAP RISK",
                    "Discounted valuation offset by deteriorating business fundamentals.",
                    SnapshotUiSupport.COLOR_AMBER, "rgba(255, 179, 0, 0.12)", "rgba(255, 179, 0, 0.35)");
        }
        if (outlook == AnalysisOutlook.BULLISH && conviction >= 4) {
            return new VerdictStance("HIGH-CONVICTION ACCUMULATE",
                    "Strong business momentum trading below estimated intrinsic value.",
                    SnapshotUiSupport.COLOR_GREEN, "rgba(46, 204, 113, 0.14)", "rgba(46, 204, 113, 0.40)");
        }
        String label = outlook == AnalysisOutlook.BULLISH ? "ACCUMULATE" : "ACCUMULATE ON VALUE";
        String why = outlook == AnalysisOutlook.BULLISH
                ? "Favorable business trajectory with valuation upside, tempered by active risk guardrails."
                : "Steady business fundamentals trading at a discount to estimated intrinsic value.";
        return new VerdictStance(label, why,
                SnapshotUiSupport.COLOR_GREEN, "rgba(46, 204, 113, 0.12)", "rgba(46, 204, 113, 0.35)");
    }

    private static VerdictStance deriveFairlyValuedStance(AnalysisOutlook outlook) {
        if (outlook == AnalysisOutlook.BEARISH) {
            return new VerdictStance("CAUTIOUS / REDUCE",
                    "Weakening business trajectory without a margin-of-safety valuation discount.",
                    SnapshotUiSupport.COLOR_AMBER, "rgba(255, 179, 0, 0.12)", "rgba(255, 179, 0, 0.35)");
        }
        if (outlook == AnalysisOutlook.BULLISH) {
            return new VerdictStance("HOLD / WATCHLIST",
                    "Strong business trajectory, but current price already assumes demanding long-term growth.",
                    SnapshotUiSupport.COLOR_BLUE, "rgba(52, 152, 219, 0.14)", "rgba(52, 152, 219, 0.38)");
        }
        return new VerdictStance("HOLD / NEUTRAL",
                "Balanced risk/reward with share price aligned near estimated intrinsic value.",
                SnapshotUiSupport.COLOR_BLUE, "rgba(52, 152, 219, 0.12)", "rgba(52, 152, 219, 0.32)");
    }

    private static VerdictStance deriveOvervaluedStance(AnalysisOutlook outlook) {
        if (outlook == AnalysisOutlook.BULLISH) {
            return new VerdictStance("WAIT FOR PULLBACK",
                    "Strong underlying business, but current share price exceeds intrinsic value benchmarks.",
                    SnapshotUiSupport.COLOR_ORANGE, "rgba(230, 126, 34, 0.14)", "rgba(230, 126, 34, 0.38)");
        }
        String label = outlook == AnalysisOutlook.BEARISH ? "AVOID / HIGH RISK" : "AVOID / OVERVALUED";
        String why = outlook == AnalysisOutlook.BEARISH
                ? "Deteriorating business fundamentals combined with stretched valuation."
                : "Share price exceeds estimated intrinsic value without sufficient growth catalysts.";
        return new VerdictStance(label, why,
                SnapshotUiSupport.COLOR_RED, "rgba(231, 76, 60, 0.14)", "rgba(231, 76, 60, 0.38)");
    }

    private Div createHeroContainer(VerdictStance stance) {
        Div hero = new Div();
        hero.setWidthFull();
        hero.getStyle()
                .set("background", "linear-gradient(180deg, rgba(var(--lumo-contrast-rgb), 0.05) 0%, rgba(var(--lumo-contrast-rgb), 0.025) 100%)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-left", "4px solid " + stance.accentColor())
                .set("border-radius", "8px")
                .set("padding", "18px 20px")
                .set("margin-bottom", "14px")
                .set("box-sizing", "border-box");
        return hero;
    }

    private HorizontalLayout buildTopHeaderRow(AnalysisResult snapshot, VerdictStance stance) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        row.setAlignItems(FlexComponent.Alignment.START);
        row.getStyle().set("gap", "20px").set("flex-wrap", "wrap");
        row.add(buildLeftVerdictColumn(snapshot, stance), buildRightMetricsCluster(snapshot));
        return row;
    }

    private Div buildLeftVerdictColumn(AnalysisResult snapshot, VerdictStance stance) {
        Div left = new Div();
        left.getStyle().set("flex", "1 1 420px");

        HorizontalLayout headlineRow = new HorizontalLayout(createPrimaryStanceBadge(stance), createWhySummary(stance.summaryWhy()));
        headlineRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headlineRow.getStyle().set("gap", "12px").set("flex-wrap", "wrap");

        left.add(headlineRow, buildBusinessVsStockRow(snapshot));
        return left;
    }

    private Span createPrimaryStanceBadge(VerdictStance stance) {
        Span badge = new Span(stance.label());
        badge.getStyle()
                .set("background", stance.bgTint())
                .set("border", "1px solid " + stance.borderTint())
                .set("color", stance.accentColor())
                .set("font-size", "0.82rem")
                .set("font-weight", "800")
                .set("letter-spacing", "0.06em")
                .set("padding", "5px 12px")
                .set("border-radius", "4px")
                .set("white-space", "nowrap");
        return badge;
    }

    private Span createWhySummary(String why) {
        Span span = new Span(why);
        span.getStyle()
                .set("font-size", "0.92rem")
                .set("font-weight", "600")
                .set("color", "var(--lumo-header-text-color)");
        return span;
    }

    private HorizontalLayout buildBusinessVsStockRow(AnalysisResult snapshot) {
        HorizontalLayout splitRow = new HorizontalLayout();
        splitRow.setAlignItems(FlexComponent.Alignment.CENTER);
        splitRow.getStyle().set("gap", "14px").set("margin-top", "10px").set("flex-wrap", "wrap");

        String outlookText = snapshot.outlook() != null ? snapshot.outlook().getDisplayName() : "Pending";
        String valText = snapshot.valuation() != null ? snapshot.valuation().getDisplayLabel() : "Pending";

        splitRow.add(
                createDotIndicator("Business Outlook:", outlookText, resolveOutlookDotColor(snapshot.outlook())),
                createSubtleSeparator(),
                createDotIndicator("Valuation Setup:", valText, resolveValuationDotColor(snapshot.valuation()))
        );
        return splitRow;
    }

    private HorizontalLayout createDotIndicator(String labelText, String valueText, String dotColor) {
        Span dot = new Span();
        dot.getStyle()
                .set("width", "8px")
                .set("height", "8px")
                .set("border-radius", "50%")
                .set("background", dotColor)
                .set("display", "inline-block");

        Span label = new Span(labelText);
        label.getStyle().set("font-size", "0.80rem").set("color", "var(--lumo-secondary-text-color)");

        Span val = new Span(valueText);
        val.getStyle().set("font-size", "0.82rem").set("font-weight", "700").set("color", "var(--lumo-header-text-color)");

        HorizontalLayout group = new HorizontalLayout(dot, label, val);
        group.setAlignItems(FlexComponent.Alignment.CENTER);
        group.getStyle().set("gap", "6px");
        return group;
    }

    private Span createSubtleSeparator() {
        Span sep = new Span("•");
        sep.getStyle().set("color", "var(--lumo-contrast-30pct)").set("font-size", "0.80rem");
        return sep;
    }

    private String resolveOutlookDotColor(AnalysisOutlook outlook) {
        if (outlook == AnalysisOutlook.BULLISH) return SnapshotUiSupport.COLOR_GREEN;
        if (outlook == AnalysisOutlook.BEARISH) return SnapshotUiSupport.COLOR_RED;
        return SnapshotUiSupport.COLOR_SLATE;
    }

    private String resolveValuationDotColor(ValuationAssessment valuation) {
        if (valuation == ValuationAssessment.UNDERVALUED) return SnapshotUiSupport.COLOR_GREEN;
        if (valuation == ValuationAssessment.OVERVALUED) return SnapshotUiSupport.COLOR_RED;
        if (valuation == ValuationAssessment.FAIRLY_VALUED) return SnapshotUiSupport.COLOR_AMBER;
        return SnapshotUiSupport.COLOR_SLATE;
    }

    private HorizontalLayout buildRightMetricsCluster(AnalysisResult snapshot) {
        HorizontalLayout right = new HorizontalLayout();
        right.setAlignItems(FlexComponent.Alignment.CENTER);
        right.getStyle().set("gap", "18px").set("flex-wrap", "wrap");

        DecisionSummary s = snapshot.decisionSummary();
        if (s != null && s.currentSharePrice() != null) {
            right.add(buildPriceBlock(s), createVerticalDivider());
        }
        right.add(buildConvictionBlock(snapshot.conviction(), snapshot.convictionReasons()));
        return right;
    }

    private Div buildPriceBlock(DecisionSummary s) {
        Div box = new Div();
        box.getStyle().set("text-align", "right");

        Span price = new Span(String.format(Locale.US, "$%.2f", s.currentSharePrice()));
        price.getStyle()
                .set("display", "block")
                .set("font-size", "1.30rem")
                .set("font-weight", "700")
                .set("line-height", "1.15")
                .set("color", "var(--lumo-header-text-color)");

        boolean isStale = s.priceStalenessDays() != null && s.priceStalenessDays() > 4;
        Span sub = new Span(formatPriceSubtitle(s, isStale));
        sub.getStyle()
                .set("display", "block")
                .set("font-size", "0.72rem")
                .set("color", isStale ? SnapshotUiSupport.COLOR_AMBER : "var(--lumo-secondary-text-color)");

        box.add(price, sub);
        return box;
    }

    private String formatPriceSubtitle(DecisionSummary s, boolean isStale) {
        String base = s.priceDate() != null ? "Ref. price • " + s.priceDate() : "Ref. share price";
        return isStale ? base + " (" + s.priceStalenessDays() + "d old)" : base;
    }

    private Span createVerticalDivider() {
        Span div = new Span();
        div.getStyle().set("width", "1px").set("height", "32px").set("background", "var(--lumo-contrast-10pct)");
        return div;
    }

    private Div buildConvictionBlock(int conviction, List<String> reasons) {
        Div box = new Div();
        box.getStyle().set("text-align", "right");

        HorizontalLayout meterRow = new HorizontalLayout(buildConvictionBars(conviction), createConvictionTitle(conviction));
        meterRow.setAlignItems(FlexComponent.Alignment.CENTER);
        meterRow.getStyle().set("gap", "8px").set("justify-content", "flex-end");

        Span sub = new Span(formatConvictionSubtitle(conviction, reasons));
        sub.getStyle()
                .set("display", "block")
                .set("font-size", "0.72rem")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-top", "2px");

        box.add(meterRow, sub);
        return box;
    }

    private HorizontalLayout buildConvictionBars(int conviction) {
        HorizontalLayout meter = new HorizontalLayout();
        meter.setSpacing(false);
        meter.getStyle().set("gap", "3px").set("align-items", "center");
        for (int i = 1; i <= 5; i++) {
            Span bar = new Span();
            bar.getStyle()
                    .set("width", "10px")
                    .set("height", "13px")
                    .set("border-radius", "2px")
                    .set("background", i <= conviction ? "var(--lumo-primary-color)" : "rgba(255, 255, 255, 0.12)");
            meter.add(bar);
        }
        return meter;
    }

    private Span createConvictionTitle(int conviction) {
        Span text = new Span("Conviction " + conviction + "/5");
        text.getStyle()
                .set("font-size", "0.88rem")
                .set("font-weight", "700")
                .set("color", "var(--lumo-header-text-color)");
        return text;
    }

    private String formatConvictionSubtitle(int conviction, List<String> reasons) {
        String level = conviction >= 4 ? "High confidence" : (conviction == 3 ? "Moderate" : "Low visibility");
        long capCount = reasons == null ? 0 : reasons.stream().filter(r -> r != null && r.startsWith("Capped")).count();
        return capCount > 0 ? level + " • " + capCount + (capCount == 1 ? " active guardrail" : " active guardrails") : level;
    }

    private void addThesisSection(Div hero, AnalysisResult snapshot, CompanyAnalysisDto analysis) {
        if (snapshot.thesis() == null || snapshot.thesis().isBlank()) {
            return;
        }
        Div thesisWrap = new Div();
        thesisWrap.getStyle()
                .set("margin-top", "14px")
                .set("padding-top", "12px")
                .set("border-top", "1px solid var(--lumo-contrast-10pct)");

        String jsonData = analysis != null ? analysis.getAnalysisData() : null;
        Component md = markdownRenderer.renderMarkdownWithCitations(snapshot.thesis(), jsonData);
        md.getStyle().set("margin", "0").set("font-size", "0.92rem").set("line-height", "1.55");
        thesisWrap.add(md);
        hero.add(thesisWrap);
    }

    private void addDecisiveQuestionStrip(Div hero, DecisionSummary s) {
        if (s == null || (isBlank(s.decisiveQuestion()) && isBlank(s.nextCatalyst()))) {
            return;
        }
        HorizontalLayout strip = createQuestionStripLayout();
        if (!isBlank(s.decisiveQuestion())) {
            strip.add(buildQuestionSubBlock("DECISIVE QUESTION", s.decisiveQuestion(), SnapshotUiSupport.COLOR_AMBER, true));
        }
        if (!isBlank(s.nextCatalyst())) {
            String cleanCatalyst = s.nextCatalyst().replaceFirst("(?i)^Next Quarter End:\\s*", "Qtr End ");
            strip.add(buildQuestionSubBlock("NEXT CATALYST", cleanCatalyst, "var(--lumo-secondary-text-color)", false));
        }
        hero.add(strip);
    }

    private HorizontalLayout createQuestionStripLayout() {
        HorizontalLayout strip = new HorizontalLayout();
        strip.setWidthFull();
        strip.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        strip.setAlignItems(FlexComponent.Alignment.CENTER);
        strip.getStyle()
                .set("margin-top", "12px")
                .set("padding", "10px 14px")
                .set("background", "rgba(255, 179, 0, 0.04)")
                .set("border", "1px solid rgba(255, 179, 0, 0.16)")
                .set("border-radius", "6px")
                .set("gap", "16px")
                .set("flex-wrap", "wrap")
                .set("box-sizing", "border-box");
        return strip;
    }

    private Div buildQuestionSubBlock(String label, String text, String labelColor, boolean grow) {
        Div box = new Div();
        box.getStyle().set("flex", grow ? "1 1 380px" : "0 0 auto").set("text-align", grow ? "left" : "right");

        Span lbl = new Span(label);
        lbl.getStyle()
                .set("display", "block")
                .set("font-size", "0.68rem")
                .set("font-weight", "700")
                .set("letter-spacing", "0.06em")
                .set("color", labelColor);

        Span val = new Span(text);
        val.getStyle()
                .set("display", "block")
                .set("font-size", grow ? "0.88rem" : "0.82rem")
                .set("font-weight", grow ? "600" : "500")
                .set("color", "var(--lumo-header-text-color)")
                .set("margin-top", "2px");

        box.add(lbl, val);
        return box;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
