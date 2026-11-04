package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.dto.valuation.OperatingEarningsSummaryDto;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class OperatingEarningsCalculatorTest {

    private OperatingEarningsCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new OperatingEarningsCalculator();
    }

    @Test
    void calculate_nullOrInvalidInputs_returnsNull() {
        assertThat(calculator.calculate(null, null)).isNull();
        assertThat(calculator.calculate(0.0f, null)).isNull();

        CompanyFinancialRatiosDto incomplete = createRatio(null, 100.0f, 40.0f, 10.0f, 2.0f, 0.0f, 0);
        assertThat(calculator.calculate(50.0f, incomplete)).isNull();
    }

    @Test
    void calculate_derivesDynamicEffectiveTaxRate_whenEbtAndNetIncomeAreValid() {
        // Revenue = 100B, OpMargin = 0.40 -> OpIncome = 40B
        // NonOpShare = 20.0% of EBT -> EBT = 40B / 0.80 = 50B
        // NetIncome = 40B -> Implied effective tax rate = 1 - 40B / 50B = 20% (0.20f)
        // OperatingNetIncome = 40B * (1 - 0.20) = 32B -> OpEPS = 3.20, Op P/E = 25.0x vs GAAP P/E = 20.0x
        CompanyFinancialRatiosDto ratio = createRatio(
                0.40f, 100_000_000_000.0f, 40_000_000_000.0f, 10_000_000_000.0f, 4.0f, 20.0f, 1
        );

        OperatingEarningsSummaryDto summary = calculator.calculate(80.0f, ratio);

        assertThat(summary).isNotNull();
        assertThat(summary.effectiveTaxRate()).isEqualTo(0.20f);
        assertThat(summary.operatingEps()).isEqualTo(3.20f);
        assertThat(summary.operatingPe()).isEqualTo(25.0f);
        assertThat(summary.gaapPe()).isEqualTo(20.0f);
        assertThat(summary.flagHighNonOperatingIncome()).isTrue();
        assertThat(summary.note()).contains("Elevated non-operating items");
    }

    @Test
    void calculate_fallsBackToDefault18PctTaxRate_whenNetIncomeMissingOrAnomalous() {
        // NetIncome is null -> falls back to 18% effective tax rate: OpIncome = 50B * 0.82 = 41B -> OpEPS = 4.10
        CompanyFinancialRatiosDto ratio = createRatio(
                0.50f, 100_000_000_000.0f, null, 10_000_000_000.0f, 4.0f, 2.0f, 0
        );

        OperatingEarningsSummaryDto summary = calculator.calculate(82.0f, ratio);

        assertThat(summary).isNotNull();
        assertThat(summary.effectiveTaxRate()).isEqualTo(0.18f);
        assertThat(summary.operatingEps()).isEqualTo(4.10f);
        assertThat(summary.operatingPe()).isEqualTo(20.0f);
        assertThat(summary.gaapPe()).isEqualTo(20.5f);
        assertThat(summary.flagHighNonOperatingIncome()).isFalse();
        assertThat(summary.note()).contains("distortion is minimal");
    }

    private CompanyFinancialRatiosDto createRatio(
            Float opMargin, Float rev, Float netIncome, Float shares, Float gaapEps, Float nonOpShare, Integer flagHighNonOp
    ) {
        return new CompanyFinancialRatiosDto(
                "ratio-ttm", 1, "NVDA", "USD", null, StatementVariant.TTM,
                2026, "TTM", LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 31),
                null, 40_000_000_000.0f, null, null, shares, null, null, null,
                null, rev, netIncome, null, opMargin, 0.40f,
                null, null, null, gaapEps, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                nonOpShare, null, null, null, null, flagHighNonOp, 0, 0
        );
    }
}
