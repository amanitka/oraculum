package com.oraculum.analyst.service.macro;

import com.oraculum.economy.api.domain.MacroIndicator;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class SectorMacroIndicatorPolicy {

    private static final Set<MacroIndicator> TECH_INDICATORS = Set.of(
            MacroIndicator.DGORDER,
            MacroIndicator.INDPRO,
            MacroIndicator.GS10,
            MacroIndicator.FEDFUNDS,
            MacroIndicator.T10Y2Y,
            MacroIndicator.TCU,
            MacroIndicator.PPIACO,
            MacroIndicator.GDPC1,
            MacroIndicator.M2SL
    );

    private static final Set<MacroIndicator> CONSUMER_DISCRETIONARY_INDICATORS = Set.of(
            MacroIndicator.UMCSENT,
            MacroIndicator.RSAFS,
            MacroIndicator.CPIAUCSL,
            MacroIndicator.UNRATE,
            MacroIndicator.FEDFUNDS,
            MacroIndicator.GDPC1,
            MacroIndicator.GS10
    );

    private static final Set<MacroIndicator> FINANCIALS_INDICATORS = Set.of(
            MacroIndicator.FEDFUNDS,
            MacroIndicator.T10Y2Y,
            MacroIndicator.GS10,
            MacroIndicator.M2SL,
            MacroIndicator.UNRATE,
            MacroIndicator.GDPC1,
            MacroIndicator.MORTGAGE30US
    );

    private static final Set<MacroIndicator> REAL_ESTATE_INDICATORS = Set.of(
            MacroIndicator.MORTGAGE30US,
            MacroIndicator.HOUST,
            MacroIndicator.GS10,
            MacroIndicator.FEDFUNDS,
            MacroIndicator.T10Y2Y,
            MacroIndicator.CPIAUCSL,
            MacroIndicator.UNRATE
    );

    private static final Set<MacroIndicator> ENERGY_INDICATORS = Set.of(
            MacroIndicator.DCOILWTICO,
            MacroIndicator.PPIACO,
            MacroIndicator.INDPRO,
            MacroIndicator.GDPC1,
            MacroIndicator.CPIAUCSL,
            MacroIndicator.GS10,
            MacroIndicator.FEDFUNDS
    );

    private static final Set<MacroIndicator> INDUSTRIALS_INDICATORS = Set.of(
            MacroIndicator.INDPRO,
            MacroIndicator.TCU,
            MacroIndicator.DGORDER,
            MacroIndicator.PPIACO,
            MacroIndicator.GDPC1,
            MacroIndicator.GS10,
            MacroIndicator.T10Y2Y,
            MacroIndicator.DCOILWTICO
    );

    private static final Set<MacroIndicator> HEALTHCARE_INDICATORS = Set.of(
            MacroIndicator.CPIAUCSL,
            MacroIndicator.GDPC1,
            MacroIndicator.UNRATE,
            MacroIndicator.GS10,
            MacroIndicator.FEDFUNDS
    );

    private static final Set<MacroIndicator> CONSUMER_STAPLES_INDICATORS = Set.of(
            MacroIndicator.CPIAUCSL,
            MacroIndicator.PPIACO,
            MacroIndicator.RSAFS,
            MacroIndicator.UNRATE,
            MacroIndicator.UMCSENT,
            MacroIndicator.GDPC1,
            MacroIndicator.FEDFUNDS
    );

    private static final Set<MacroIndicator> UTILITIES_AND_COMMUNICATION_INDICATORS = Set.of(
            MacroIndicator.GS10,
            MacroIndicator.FEDFUNDS,
            MacroIndicator.T10Y2Y,
            MacroIndicator.CPIAUCSL,
            MacroIndicator.GDPC1,
            MacroIndicator.UNRATE
    );

    private static final Set<MacroIndicator> DEFAULT_INDICATORS = Set.of(
            MacroIndicator.FEDFUNDS,
            MacroIndicator.GS10,
            MacroIndicator.T10Y2Y,
            MacroIndicator.GDPC1,
            MacroIndicator.CPIAUCSL,
            MacroIndicator.UNRATE,
            MacroIndicator.INDPRO,
            MacroIndicator.M2SL,
            MacroIndicator.UMCSENT,
            MacroIndicator.RSAFS,
            MacroIndicator.DGORDER
    );

    private static final Map<String, Set<MacroIndicator>> SECTOR_MAP = Map.ofEntries(
            Map.entry("technology", TECH_INDICATORS),
            Map.entry("information technology", TECH_INDICATORS),
            Map.entry("semiconductors", TECH_INDICATORS),
            Map.entry("consumer cyclical", CONSUMER_DISCRETIONARY_INDICATORS),
            Map.entry("consumer discretionary", CONSUMER_DISCRETIONARY_INDICATORS),
            Map.entry("financials", FINANCIALS_INDICATORS),
            Map.entry("financial services", FINANCIALS_INDICATORS),
            Map.entry("real estate", REAL_ESTATE_INDICATORS),
            Map.entry("energy", ENERGY_INDICATORS),
            Map.entry("oil & gas", ENERGY_INDICATORS),
            Map.entry("industrials", INDUSTRIALS_INDICATORS),
            Map.entry("basic materials", INDUSTRIALS_INDICATORS),
            Map.entry("materials", INDUSTRIALS_INDICATORS),
            Map.entry("healthcare", HEALTHCARE_INDICATORS),
            Map.entry("consumer defensive", CONSUMER_STAPLES_INDICATORS),
            Map.entry("consumer staples", CONSUMER_STAPLES_INDICATORS),
            Map.entry("utilities", UTILITIES_AND_COMMUNICATION_INDICATORS),
            Map.entry("communication services", UTILITIES_AND_COMMUNICATION_INDICATORS)
    );

    public Set<MacroIndicator> getRelevantIndicators(String sectorName) {
        if (sectorName == null || sectorName.isBlank()) {
            return DEFAULT_INDICATORS;
        }
        String normalized = sectorName.trim().toLowerCase(Locale.ROOT);
        return SECTOR_MAP.getOrDefault(normalized, DEFAULT_INDICATORS);
    }
}
