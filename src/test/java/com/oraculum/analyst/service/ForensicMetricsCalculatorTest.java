package com.oraculum.analyst.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ForensicMetricsCalculatorTest {

    private ForensicMetricsCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ForensicMetricsCalculator();
    }

    @Test
    void calculateNonOperatingIncomeSharePretaxPct_normalValues_computesCorrectly() {
        Float pct = calculator.calculateNonOperatingIncomeSharePretaxPct(100.0f, 80.0f);

        assertThat(pct).isNotNull();
        assertThat(pct).isCloseTo(20.0f, within(0.01f));
    }

    @Test
    void calculateNonOperatingIncomeSharePretaxPct_negativeEbt_usesAbsoluteDenominator() {
        Float pct = calculator.calculateNonOperatingIncomeSharePretaxPct(-50.0f, -70.0f);

        assertThat(pct).isNotNull();
        assertThat(pct).isCloseTo(40.0f, within(0.01f));
    }

    @Test
    void calculateNonOperatingIncomeSharePretaxPct_nullOrZero_returnsNull() {
        assertThat(calculator.calculateNonOperatingIncomeSharePretaxPct(null, 50.0f)).isNull();
        assertThat(calculator.calculateNonOperatingIncomeSharePretaxPct(0.0f, 50.0f)).isNull();
        assertThat(calculator.calculateNonOperatingIncomeSharePretaxPct(100.0f, null)).isNull();
    }

    @Test
    void calculateShareholderPayoutToFcfRatioPct_validValues_computesPercentage() {
        Float pct = calculator.calculateShareholderPayoutToFcfRatioPct(500.0f, -300.0f, -100.0f);

        assertThat(pct).isNotNull();
        assertThat(pct).isCloseTo(80.0f, within(0.01f));
    }

    @Test
    void calculateShareholderPayoutToFcfRatioPct_nonPositiveFcf_returnsNull() {
        assertThat(calculator.calculateShareholderPayoutToFcfRatioPct(0.0f, -100.0f, 0.0f)).isNull();
        assertThat(calculator.calculateShareholderPayoutToFcfRatioPct(-100.0f, -100.0f, 0.0f)).isNull();
        assertThat(calculator.calculateShareholderPayoutToFcfRatioPct(null, -100.0f, 0.0f)).isNull();
    }

    @Test
    void calculateNonCapexInvestingOutflow_computesDifferenceBetweenInvestingAndCapex() {
        Float outflow = calculator.calculateNonCapexInvestingOutflow(-1500.0f, -500.0f);

        assertThat(outflow).isNotNull();
        assertThat(outflow).isCloseTo(1000.0f, within(0.01f));
    }

    @Test
    void calculateNonCapexInvestingOutflow_nullInput_returnsNull() {
        assertThat(calculator.calculateNonCapexInvestingOutflow(null, -500.0f)).isNull();
        assertThat(calculator.calculateNonCapexInvestingOutflow(-1500.0f, null)).isNull();
    }

    @Test
    void calculateNonCapexInvestingToFcfRatioPct_computesRatio() {
        Float ratio = calculator.calculateNonCapexInvestingToFcfRatioPct(1000.0f, 2000.0f);

        assertThat(ratio).isNotNull();
        assertThat(ratio).isCloseTo(50.0f, within(0.01f));
    }
}
