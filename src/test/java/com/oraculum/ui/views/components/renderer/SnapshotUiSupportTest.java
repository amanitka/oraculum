package com.oraculum.ui.views.components.renderer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotUiSupportTest {

    @Test
    void resolveScoreBadgeLabel_forRiskDimension_usesInvertedRiskLabels() {
        assertThat(SnapshotUiSupport.resolveScoreBadgeLabel("Risk Profile", 8.0)).isEqualTo("Low Risk");
        assertThat(SnapshotUiSupport.resolveScoreBadgeLabel("Risk", 5.5)).isEqualTo("Moderate Risk");
        assertThat(SnapshotUiSupport.resolveScoreBadgeLabel("risk_profile", 4.0)).isEqualTo("Elevated Risk");
        assertThat(SnapshotUiSupport.resolveScoreBadgeLabel("Risk", 2.0)).isEqualTo("High Risk");
    }

    @Test
    void resolveDimensionTheme_mapsRiskBadgeLabelsToExpectedThemes() {
        assertThat(SnapshotUiSupport.resolveDimensionTheme("Low Risk")).isEqualTo("success");
        assertThat(SnapshotUiSupport.resolveDimensionTheme("Moderate Risk")).isEqualTo("contrast");
        assertThat(SnapshotUiSupport.resolveDimensionTheme("Elevated Risk")).isEqualTo("warning");
        assertThat(SnapshotUiSupport.resolveDimensionTheme("High Risk")).isEqualTo("error");
    }

    @Test
    void resolveScoreBadgeLabel_forNonRiskDimension_usesQualityLabels() {
        assertThat(SnapshotUiSupport.resolveScoreBadgeLabel("Fundamentals", 9.0)).isEqualTo("Exceptional");
        assertThat(SnapshotUiSupport.resolveScoreBadgeLabel("Valuation", 7.2)).isEqualTo("Strong");
        assertThat(SnapshotUiSupport.resolveScoreBadgeLabel("Cash Flow", 5.5)).isEqualTo("Moderate");
        assertThat(SnapshotUiSupport.resolveScoreBadgeLabel("News", 4.0)).isEqualTo("Vulnerable");
    }

    @Test
    void resolveScoreBadgeTheme_mapsHigherIsBetterScoresToExpectedThemes() {
        assertThat(SnapshotUiSupport.resolveScoreBadgeTheme(8.0)).isEqualTo("success");
        assertThat(SnapshotUiSupport.resolveScoreBadgeTheme(5.5)).isEqualTo("contrast");
        assertThat(SnapshotUiSupport.resolveScoreBadgeTheme(4.0)).isEqualTo("warning");
        assertThat(SnapshotUiSupport.resolveScoreBadgeTheme(2.0)).isEqualTo("error");
    }
}
