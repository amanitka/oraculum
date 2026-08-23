package com.oraculum.ui.views.components.renderer;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class AnalysisOverviewRendererTest {

    @Test
    void normalizeFactorScores_scalesUpWhenAllScoresBelowOrEqualToOne() {
        Map<String, Double> input = Map.of(
                "fundamental_health", 0.78,
                "valuation", 0.79,
                "growth_prospects", 0.65,
                "risk_profile", 0.65
        );

        Map<String, Double> result = AnalysisOverviewRenderer.normalizeFactorScores(input);

        assertThat(result.get("fundamental_health")).isCloseTo(7.8, offset(0.001));
        assertThat(result.get("valuation")).isCloseTo(7.9, offset(0.001));
        assertThat(result.get("growth_prospects")).isCloseTo(6.5, offset(0.001));
        assertThat(result.get("risk_profile")).isCloseTo(6.5, offset(0.001));
    }

    @Test
    void normalizeFactorScores_preservesLowScoreWhenStandardScaleUsed() {
        // If max score > 1.0 (e.g. 8.5), a genuine low score like 0.8 is preserved and NOT multiplied by 10
        Map<String, Double> input = Map.of(
                "fundamental_health", 8.5,
                "valuation", 0.8,
                "growth_prospects", 7.0,
                "risk_profile", 6.5
        );

        Map<String, Double> result = AnalysisOverviewRenderer.normalizeFactorScores(input);

        assertThat(result.get("fundamental_health")).isCloseTo(8.5, offset(0.001));
        assertThat(result.get("valuation")).isCloseTo(0.8, offset(0.001));
        assertThat(result.get("growth_prospects")).isCloseTo(7.0, offset(0.001));
        assertThat(result.get("risk_profile")).isCloseTo(6.5, offset(0.001));
    }

    @Test
    void normalizeFactorScores_scalesDownWhenHundredScaleUsed() {
        Map<String, Double> input = Map.of(
                "fundamental_health", 85.0,
                "valuation", 79.0,
                "growth_prospects", 65.0,
                "risk_profile", 65.0
        );

        Map<String, Double> result = AnalysisOverviewRenderer.normalizeFactorScores(input);

        assertThat(result.get("fundamental_health")).isCloseTo(8.5, offset(0.001));
        assertThat(result.get("valuation")).isCloseTo(7.9, offset(0.001));
        assertThat(result.get("growth_prospects")).isCloseTo(6.5, offset(0.001));
        assertThat(result.get("risk_profile")).isCloseTo(6.5, offset(0.001));
    }

    @Test
    void normalizeFactorScores_handlesNullAndEmptyMaps() {
        assertThat(AnalysisOverviewRenderer.normalizeFactorScores(null)).isEmpty();
        assertThat(AnalysisOverviewRenderer.normalizeFactorScores(Map.of())).isEmpty();
    }

    @Test
    void normalizeFactorScores_handlesNullValuesAndAllZeros() {
        Map<String, Double> input = new LinkedHashMap<>();
        input.put("fundamental_health", null);
        input.put("valuation", 0.0);

        Map<String, Double> result = AnalysisOverviewRenderer.normalizeFactorScores(input);

        assertThat(result.get("fundamental_health")).isEqualTo(0.0);
        assertThat(result.get("valuation")).isEqualTo(0.0);
    }
}
