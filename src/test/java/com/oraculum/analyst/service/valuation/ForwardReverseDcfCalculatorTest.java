package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.dto.valuation.ForwardReverseDcfDto;
import com.oraculum.analyst.dto.valuation.ForwardReverseDcfRequest;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.ValuationZone;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ForwardReverseDcfCalculatorTest {

    private ForwardReverseDcfCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ForwardReverseDcfCalculator();
    }

    @Test
    void calculate_nullOrInvalidInputs_returnsNull() {
        assertThat(calculator.calculate(null)).isNull();
        assertThat(calculator.calculate(ForwardReverseDcfRequest.builder().marketCap(0.0).build())).isNull();
        assertThat(calculator.calculate(ForwardReverseDcfRequest.builder().marketCap(1_000_000.0).build())).isNull();
    }

    @Test
    void calculate_solvesImpliedGrowthAndUsesExactFractionalHorizonForConsensusCagr() {
        LocalDate analysisDate = LocalDate.of(2026, 10, 28);
        EarningsEstimateDto fy1 = createEstimate(LocalDate.of(2027, 1, 31), 160_000_000_000.0, 6.0);
        EarningsEstimateDto fy2 = createEstimate(LocalDate.of(2028, 1, 31), 210_000_000_000.0, 8.0);

        List<CompanyFinancialRatiosDto> annual = List.of(
                createRatio(2026, 0.45f, 60_000_000_000.0f, 60_000_000_000.0f, 0.40f),
                createRatio(2025, 0.45f, 40_000_000_000.0f, 40_000_000_000.0f, 0.50f)
        );
        CompanyFinancialRatiosDto ttm = createRatio(2026, 0.45f, 70_000_000_000.0f, 70_000_000_000.0f, 0.45f);

        ForwardReverseDcfRequest request = ForwardReverseDcfRequest.builder()
                .marketCap(3_500_000_000_000.0)
                .currentSharePrice(140.0f)
                .analysisDate(analysisDate)
                .fy1(fy1)
                .fy2(fy2)
                .annualRatios(annual)
                .ttmRatio(ttm)
                .build();

        ForwardReverseDcfDto result = calculator.calculate(request);

        assertThat(result).isNotNull();
        assertThat(result.impliedGrowthYears3To10Pct()).isNotNull();
        assertThat(result.relativeValuationZone()).isNotNull();
        assertThat(result.fy2FcfProxyUsd()).isGreaterThan(0.0);
        // t2 = ~1.26y (Oct 2026 -> Jan 2028), rev2/rev0 = 210B/150B = 1.40 -> 1.40^(1/1.26) - 1 = ~30.6% (vs 18.3% with hardcoded sqrt)
        assertThat(result.consensusGrowthRatePct()).isCloseTo(30.6f, within(0.5f));
        assertThat(result.interpretation()).isNotBlank();
        assertThat(result.isClipped()).isFalse();
    }

    @Test
    void calculate_extremeMarketCap_clipsAtUpperBound() {
        LocalDate analysisDate = LocalDate.of(2026, 1, 1);
        List<EarningsEstimateDto> estimates = List.of(
                createEstimate(LocalDate.of(2026, 12, 31), 1_000_000.0, 1.0),
                createEstimate(LocalDate.of(2027, 12, 31), 1_200_000.0, 1.2)
        );
        List<CompanyFinancialRatiosDto> annual = List.of(createRatio(2025, 0.20f, 200_000.0f, 200_000.0f, 0.10f));

        ForwardReverseDcfRequest request = ForwardReverseDcfRequest.builder()
                .marketCap(100_000_000_000_000.0)
                .currentSharePrice(100.0f)
                .analysisDate(analysisDate)
                .estimates(estimates)
                .annualRatios(annual)
                .build();

        ForwardReverseDcfDto result = calculator.calculate(request);

        assertThat(result).isNotNull();
        assertThat(result.isClipped()).isTrue();
        assertThat(result.relativeValuationZone()).isEqualTo(ValuationZone.SPECULATIVE);
    }

    private EarningsEstimateDto createEstimate(LocalDate date, Double revAvg, Double epsAvg) {
        return new EarningsEstimateDto(
                date, "fiscal year", BigDecimal.valueOf(epsAvg), null, null,
                null, null, null, null, null, null, null, null, null,
                BigDecimal.valueOf(revAvg), null, null, null
        );
    }

    private CompanyFinancialRatiosDto createRatio(
            int year, Float fcfMargin, Float fcf, Float netIncome, Float fcfGrowth
    ) {
        return new CompanyFinancialRatiosDto(
                "ratio-" + year, 1, "NVDA", "USD", null, StatementVariant.ANNUAL,
                year, "FY", LocalDate.of(year, 1, 31), LocalDate.of(year, 1, 31), LocalDate.of(year, 1, 31),
                null, fcf, null, null, 25_000_000_000.0f, null, null, null,
                null, 150_000_000_000.0f, netIncome, null, 0.60f, fcfMargin,
                null, null, null, 6.0f, null, null, null,
                null, null, null, fcfGrowth, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 0, 0
        );
    }
}
