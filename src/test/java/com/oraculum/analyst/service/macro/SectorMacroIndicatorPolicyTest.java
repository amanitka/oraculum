package com.oraculum.analyst.service.macro;

import com.oraculum.economy.api.domain.MacroIndicator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SectorMacroIndicatorPolicyTest {

    private SectorMacroIndicatorPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new SectorMacroIndicatorPolicy();
    }

    @Test
    void getRelevantIndicators_forTechnology_shouldIncludeTechSeriesAndExcludeHousing() {
        Set<MacroIndicator> indicators = policy.getRelevantIndicators("Technology");

        assertThat(indicators).contains(
                MacroIndicator.DGORDER,
                MacroIndicator.INDPRO,
                MacroIndicator.GS10,
                MacroIndicator.FEDFUNDS,
                MacroIndicator.T10Y2Y,
                MacroIndicator.TCU,
                MacroIndicator.PPIACO
        );
        assertThat(indicators).doesNotContain(
                MacroIndicator.HOUST,
                MacroIndicator.MORTGAGE30US
        );
    }

    @Test
    void getRelevantIndicators_forSemiconductors_shouldMatchTechnology() {
        Set<MacroIndicator> tech = policy.getRelevantIndicators("Technology");
        Set<MacroIndicator> semis = policy.getRelevantIndicators("Semiconductors");

        assertThat(semis).isEqualTo(tech);
    }

    @Test
    void getRelevantIndicators_forConsumerDiscretionary_shouldIncludeConsumerSeries() {
        Set<MacroIndicator> indicators = policy.getRelevantIndicators("Consumer Discretionary");

        assertThat(indicators).contains(
                MacroIndicator.UMCSENT,
                MacroIndicator.RSAFS,
                MacroIndicator.CPIAUCSL,
                MacroIndicator.UNRATE
        );
        assertThat(indicators).doesNotContain(
                MacroIndicator.HOUST,
                MacroIndicator.MORTGAGE30US,
                MacroIndicator.TCU
        );
    }

    @Test
    void getRelevantIndicators_forRealEstate_shouldIncludeHousingAndMortgage() {
        Set<MacroIndicator> indicators = policy.getRelevantIndicators("Real Estate");

        assertThat(indicators).contains(
                MacroIndicator.MORTGAGE30US,
                MacroIndicator.HOUST,
                MacroIndicator.GS10
        );
        assertThat(indicators).doesNotContain(
                MacroIndicator.DGORDER,
                MacroIndicator.TCU
        );
    }

    @Test
    void getRelevantIndicators_forEnergy_shouldIncludeOilAndPpi() {
        Set<MacroIndicator> indicators = policy.getRelevantIndicators("Energy");

        assertThat(indicators).contains(
                MacroIndicator.DCOILWTICO,
                MacroIndicator.PPIACO,
                MacroIndicator.INDPRO
        );
        assertThat(indicators).doesNotContain(
                MacroIndicator.HOUST,
                MacroIndicator.MORTGAGE30US
        );
    }

    @Test
    void getRelevantIndicators_forNullOrBlank_shouldReturnDefaultIndicators() {
        Set<MacroIndicator> forNull = policy.getRelevantIndicators(null);
        Set<MacroIndicator> forBlank = policy.getRelevantIndicators("   ");

        assertThat(forNull).isNotEmpty();
        assertThat(forBlank).isEqualTo(forNull);
        assertThat(forNull).contains(
                MacroIndicator.FEDFUNDS,
                MacroIndicator.GS10,
                MacroIndicator.T10Y2Y,
                MacroIndicator.GDPC1
        );
    }
}
