package com.oraculum.ui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ViewHelperTest {

    @Test
    void normalizeFundName_shouldCleanSecNoiseAndLegalSuffixes() {
        assertThat(ViewHelper.normalizeFundName("FAIRFAX FINANCIAL HOLDINGS LTD/ CAN")).isEqualTo("Fairfax Financial Holdings");
        assertThat(ViewHelper.normalizeFundName("DEERFIELD MANAGEMENT COMPANY, L.P.")).isEqualTo("Deerfield Management");
        assertThat(ViewHelper.normalizeFundName("RENAISSANCE TECHNOLOGIES LLC")).isEqualTo("Renaissance Technologies");
        assertThat(ViewHelper.normalizeFundName("BAUPOST GROUP LLC/MA")).isEqualTo("Baupost Group");
        assertThat(ViewHelper.normalizeFundName("Berkshire Hathaway Inc")).isEqualTo("Berkshire Hathaway");
        assertThat(ViewHelper.normalizeFundName("THIRD AVENUE MANAGEMENT LLC")).isEqualTo("Third Avenue Management");
        assertThat(ViewHelper.normalizeFundName("TIGER GLOBAL MANAGEMENT LLC")).isEqualTo("Tiger Global Management");
        assertThat(ViewHelper.normalizeFundName("Pershing Square Capital Management, L.P.")).isEqualTo("Pershing Square Capital Management");
        assertThat(ViewHelper.normalizeFundName("Bridgewater Associates, LP")).isEqualTo("Bridgewater Associates");
        assertThat(ViewHelper.normalizeFundName("CITADEL ADVISORS LLC")).isEqualTo("Citadel Advisors");
        assertThat(ViewHelper.normalizeFundName("Duquesne Family Office LLC")).isEqualTo("Duquesne Family Office");
        assertThat(ViewHelper.normalizeFundName("Fundsmith LLP")).isEqualTo("Fundsmith");
        assertThat(ViewHelper.normalizeFundName("Scion Asset Management, LLC")).isEqualTo("Scion Asset Management");
    }
}
