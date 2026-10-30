package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.ValuationAssessment;
import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VerdictHeroRendererTest {

    private VerdictHeroRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new VerdictHeroRenderer(new MarkdownRenderer(new JsonMapper()));
    }

    @Test
    void renderHeroCard_nullSnapshot_returnsNull() {
        assertThat(renderer.renderHeroCard(null, null)).isNull();
    }

    @Test
    void deriveStance_mapsAllValuationAndOutlookCombinations() {
        assertThat(VerdictHeroRenderer.deriveStance(ValuationAssessment.UNDERVALUED, AnalysisOutlook.BULLISH, 4).label())
                .isEqualTo("HIGH-CONVICTION ACCUMULATE");
        assertThat(VerdictHeroRenderer.deriveStance(ValuationAssessment.UNDERVALUED, AnalysisOutlook.BULLISH, 3).label())
                .isEqualTo("ACCUMULATE");
        assertThat(VerdictHeroRenderer.deriveStance(ValuationAssessment.UNDERVALUED, AnalysisOutlook.NEUTRAL, 3).label())
                .isEqualTo("ACCUMULATE ON VALUE");
        assertThat(VerdictHeroRenderer.deriveStance(ValuationAssessment.UNDERVALUED, AnalysisOutlook.BEARISH, 3).label())
                .isEqualTo("WATCHLIST • VALUE TRAP RISK");

        assertThat(VerdictHeroRenderer.deriveStance(ValuationAssessment.FAIRLY_VALUED, AnalysisOutlook.BULLISH, 3).label())
                .isEqualTo("HOLD / WATCHLIST");
        assertThat(VerdictHeroRenderer.deriveStance(ValuationAssessment.FAIRLY_VALUED, AnalysisOutlook.NEUTRAL, 3).label())
                .isEqualTo("HOLD / NEUTRAL");
        assertThat(VerdictHeroRenderer.deriveStance(ValuationAssessment.FAIRLY_VALUED, AnalysisOutlook.BEARISH, 3).label())
                .isEqualTo("CAUTIOUS / REDUCE");

        assertThat(VerdictHeroRenderer.deriveStance(ValuationAssessment.OVERVALUED, AnalysisOutlook.BULLISH, 4).label())
                .isEqualTo("WAIT FOR PULLBACK");
        assertThat(VerdictHeroRenderer.deriveStance(ValuationAssessment.OVERVALUED, AnalysisOutlook.NEUTRAL, 3).label())
                .isEqualTo("AVOID / OVERVALUED");
        assertThat(VerdictHeroRenderer.deriveStance(ValuationAssessment.OVERVALUED, AnalysisOutlook.BEARISH, 2).label())
                .isEqualTo("AVOID / HIGH RISK");

        assertThat(VerdictHeroRenderer.deriveStance(ValuationAssessment.UNCERTAIN, AnalysisOutlook.NEUTRAL, 2).label())
                .isEqualTo("MONITOR / INCONCLUSIVE");
    }

    @Test
    void renderHeroCard_separatesBusinessOutlookFromValuationSetupAndShowsCatalyst() {
        DecisionSummary summary = DecisionSummary.builder()
                .currentSharePrice(210.96f)
                .priceDate(LocalDate.of(2026, 10, 23))
                .priceStalenessDays(4)
                .decisiveQuestion("Can FCF compound at ≥16.7% over 10 years?")
                .nextCatalyst("Next Quarter End: 2026-10-31 (Consensus EPS $2.47)")
                .build();

        AnalysisResult snapshot = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .outlook(AnalysisOutlook.BULLISH)
                .conviction(3)
                .thesis("Exceptional margins offset by demanding 16.7% reverse DCF hurdle.")
                .decisionSummary(summary)
                .convictionReasons(List.of(
                        "Base conviction 4/5 from specialist dispersion",
                        "Capped at 3/5: Valuation method conflict",
                        "Capped at 3/5: High EPS dispersion"
                ))
                .build();

        Component hero = renderer.renderHeroCard(snapshot, null);

        assertThat(hero).isNotNull();
        String text = hero.getElement().getTextRecursively();
        assertThat(text)
                .contains("HOLD / WATCHLIST")
                .contains("Business Outlook:")
                .contains("Bullish")
                .contains("Valuation Setup:")
                .contains("Fairly Valued")
                .contains("$210.96")
                .contains("Ref. price • 2026-10-23")
                .doesNotContain("old)")
                .contains("Conviction 3/5")
                .contains("Moderate • 2 active guardrails")
                .contains("DECISIVE QUESTION")
                .contains("Can FCF compound at ≥16.7% over 10 years?")
                .contains("NEXT CATALYST")
                .contains("Qtr End 2026-10-31 (Consensus EPS $2.47)");
    }

    @Test
    void renderHeroCard_stalePriceAboveFourDays_displaysStaleAgeInSubtitle() {
        DecisionSummary staleSummary = DecisionSummary.builder()
                .currentSharePrice(195.50f)
                .priceDate(LocalDate.of(2026, 10, 20))
                .priceStalenessDays(6)
                .build();

        AnalysisResult snapshot = AnalysisResult.builder()
                .valuation(ValuationAssessment.UNDERVALUED)
                .outlook(AnalysisOutlook.BULLISH)
                .conviction(4)
                .decisionSummary(staleSummary)
                .build();

        Component hero = renderer.renderHeroCard(snapshot, null);

        assertThat(hero.getElement().getTextRecursively())
                .contains("HIGH-CONVICTION ACCUMULATE")
                .contains("Ref. price • 2026-10-20 (6d old)");
    }
}
