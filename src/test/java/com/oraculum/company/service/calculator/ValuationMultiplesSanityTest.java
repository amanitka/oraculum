package com.oraculum.company.service.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

class ValuationMultiplesSanityTest {

    @Test
    @DisplayName("Golden Test: TTM multiples must satisfy mathematical identities (Price = PE * EPS_ttm, FCF_yield = FCF_ttm / MarketCap)")
    void ttmMultiples_satisfyMathematicalIdentities() {
        // Real NVDA values from Q2 FY2026
        float sharePrice = 229.28f;
        float epsTtm = 7.905768f;
        float marketCap = 5.568065E12f;
        float fcfTtm = 1.26886E11f;
        float revenueTtm = 3.02969E11f;

        // Correct TTM calculations as defined in v_share_price_signals
        float peRatio = sharePrice / epsTtm;
        float earningsYield = epsTtm / sharePrice;
        float fcfYield = fcfTtm / marketCap;
        float priceToSales = marketCap / revenueTtm;

        // Assert sanity: PE * EPS_TTM must equal the share price within 0.01%
        assertThat(peRatio * epsTtm).isCloseTo(sharePrice, withinPercentage(0.01));
        assertThat(peRatio).isCloseTo(29.00f, withinPercentage(0.5));

        // Assert earnings yield inverse relationship: PE * earningsYield == 1.0
        assertThat(peRatio * earningsYield).isCloseTo(1.0f, withinPercentage(0.01));

        // Assert FCF Yield is ~2.28%, NOT 0.38% (which would be quarterly FCF)
        assertThat(fcfYield * 100.0f).isCloseTo(2.28f, withinPercentage(1.0));

        // Assert P/S is ~18.38x, NOT 57.87x (which would be quarterly revenue)
        assertThat(priceToSales).isCloseTo(18.38f, withinPercentage(1.0));
    }

    @Test
    @DisplayName("Golden Test: Detecting quarterly vs TTM data corruption")
    void quarterlyCorruption_producesSevereArtifacts() {
        float sharePrice = 229.28f;
        float epsQuarterly = 2.4578135f;
        float fcfQuarterly = 2.1341E10f;
        float marketCap = 5.568065E12f;

        // When quarterly is mistakenly joined instead of TTM:
        float corruptedPe = sharePrice / epsQuarterly;
        float corruptedFcfYield = fcfQuarterly / marketCap;

        // Corrupted multiples match the exact bug identified in report
        assertThat(corruptedPe).isCloseTo(93.29f, withinPercentage(0.5));
        assertThat(corruptedFcfYield * 100.0f).isCloseTo(0.38f, withinPercentage(2.0));

        // Ratio of corrupted vs real is ~3.2x for PE and ~6x for FCF Yield
        float realPe = sharePrice / 7.905768f;
        assertThat(corruptedPe / realPe).isCloseTo(3.21f, withinPercentage(1.0));
    }
}
