package com.oraculum.analyst.api.domain;

import com.oraculum.company.api.domain.StatementVariant;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Defines the semantic data intent for each agent type.
 * Each profile declares WHICH financial data views are needed and HOW MANY periods
 * of each view should be passed to the agent's prompt.
 * <p>
 * Period limits here are secondary filters on top of the global date-based limits
 * defined in {@code AnalystProperties.FactSheet}. The DB loads data up to the global
 * horizon; each profile then slices off only what the agent actually needs.
 * <p>
 * Use {@link Integer#MAX_VALUE} to indicate "no additional limit — use all data loaded by DB".
 */
public enum FinancialDataProfile {

    /**
     * Annual (last 5 years) for multi-year trend + quarterly (all periods) for sequential momentum.
     * Used by agents analyzing growth trajectory and profitability evolution over time.
     */
    HISTORICAL_OPERATING(
            DataWindow.of(StatementVariant.ANNUAL, 5),
            DataWindow.of(StatementVariant.QUARTERLY, Integer.MAX_VALUE)
    ),

    /**
     * TTM (all periods) for normalized run-rate valuation + annual (last 5 years) for long-term
     * business quality benchmarking. Used by agents computing valuation multiples.
     */
    CURRENT_VALUATION(
            DataWindow.of(StatementVariant.ANNUAL, 5),
            DataWindow.of(StatementVariant.TTM, Integer.MAX_VALUE)
    ),

    /**
     * Annual (last 5 years) for long-term solvency signals + TTM (all periods) for current
     * leverage and liquidity. Used by risk-focused agents.
     */
    BALANCE_SHEET_RISK(
            DataWindow.of(StatementVariant.ANNUAL, 5),
            DataWindow.of(StatementVariant.TTM, Integer.MAX_VALUE)
    ),

    /**
     * TTM only (all periods) — captures the full trailing cycle of cash inflows/outflows.
     * Used by agents evaluating FCF quality and capex intensity.
     */
    CASH_GENERATION(
            DataWindow.of(StatementVariant.TTM, Integer.MAX_VALUE)
    ),

    /**
     * No structured financial statements needed. Used by market/sentiment agents
     * (News, SharePrice, EarningsEstimates) that operate on real-time signals only.
     */
    MARKET_SIGNALS();

    private final Map<StatementVariant, Integer> windows;

    FinancialDataProfile(DataWindow... windows) {
        this.windows = Arrays.stream(windows)
                .collect(Collectors.toMap(DataWindow::variant, DataWindow::maxPeriods));
    }

    /**
     * Returns the set of {@link StatementVariant}s this profile requires.
     */
    public Set<StatementVariant> variants() {
        return windows.keySet();
    }

    /**
     * Returns the maximum number of periods to send to the prompt for the given variant.
     * Defaults to {@link Integer#MAX_VALUE} if the variant is not explicitly configured.
     */
    public int periodLimit(StatementVariant variant) {
        return windows.getOrDefault(variant, Integer.MAX_VALUE);
    }

    /**
     * A simple pair of a {@link StatementVariant} and its period limit.
     * Keeps the variant and its constraint co-located and self-documenting.
     */
    public record DataWindow(StatementVariant variant, int maxPeriods) {
        public static DataWindow of(StatementVariant variant, int maxPeriods) {
            return new DataWindow(variant, maxPeriods);
        }
    }
}
