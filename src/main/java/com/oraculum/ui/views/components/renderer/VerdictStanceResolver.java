package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.ValuationAssessment;

/**
 * Pure domain-to-stance mapping separating business trajectory from stock valuation.
 */
public final class VerdictStanceResolver {

    public record VerdictStance(String label, String summaryWhy, String accentColor, String bgTint, String borderTint) {
    }

    private VerdictStanceResolver() {
    }

    public static VerdictStance deriveStance(ValuationAssessment valuation, AnalysisOutlook outlook, int conviction) {
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
}
