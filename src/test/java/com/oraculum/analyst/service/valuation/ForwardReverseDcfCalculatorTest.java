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

    @Test
    void calculate_analyticalBisectionConvergence_recoversExactTargetGrowthRate() {
        // Exact 1.0Y and 2.0Y horizons: FY1 FCF = 100, FY2 FCF = 120, target Stage-2 g = 8.0%
        LocalDate analysisDate = LocalDate.of(2026, 1, 1);
        LocalDate fy1Date = analysisDate.plusDays(365);
        LocalDate fy2Date = analysisDate.plusDays(730);

        double r = 0.10;
        double gTerm = 0.03;
        double targetG = 0.08;
        double fcf1 = 100_000_000.0;
        double fcf2 = 120_000_000.0;

        double exactPv = fcf1 / Math.pow(1.0 + r, 1.0) + fcf2 / Math.pow(1.0 + r, 2.0);
        double fcfk = fcf2;
        for (int k = 3; k <= 10; k++) {
            fcfk *= (1.0 + targetG);
            exactPv += fcfk / Math.pow(1.0 + r, k);
        }
        double tv10 = (fcfk * (1.0 + gTerm)) / (r - gTerm);
        exactPv += tv10 / Math.pow(1.0 + r, 10.0);

        ForwardReverseDcfRequest request = ForwardReverseDcfRequest.builder()
                .marketCap(exactPv)
                .currentSharePrice(50.0f)
                .analysisDate(analysisDate)
                .fy1(createEstimate(fy1Date, 500_000_000.0, 5.0))
                .fy2(createEstimate(fy2Date, 600_000_000.0, 6.0))
                .annualRatios(List.of(createRatio(2025, 0.20f, 100_000_000.0f, 100_000_000.0f, 0.15f)))
                .build();

        ForwardReverseDcfDto result = calculator.calculate(request);

        assertThat(result).isNotNull();
        assertThat(result.impliedGrowthYears3To10Pct()).isCloseTo(8.0f, within(0.1f));
        assertThat(result.isClipped()).isFalse();
    }

    @Test
    void calculate_missingFy2AndFy1AlmostElapsed_fallsBackToFy1ProxyAndClampsStubWeight() {
        LocalDate analysisDate = LocalDate.of(2026, 12, 25);
        // Only 6 days left in FY1 (< 5% of year) and no FY2 estimate available
        EarningsEstimateDto fy1Only = createEstimate(LocalDate.of(2026, 12, 31), 10_000_000_000.0, 4.0);
        List<CompanyFinancialRatiosDto> annual = List.of(createRatio(2025, 0.25f, 2_500_000_000.0f, 2_500_000_000.0f, 0.12f));

        ForwardReverseDcfRequest request = ForwardReverseDcfRequest.builder()
                .marketCap(40_000_000_000.0)
                .currentSharePrice(80.0f)
                .analysisDate(analysisDate)
                .fy1(fy1Only)
                .annualRatios(annual)
                .build();

        ForwardReverseDcfDto result = calculator.calculate(request);

        assertThat(result).isNotNull();
        assertThat(result.fy1FcfProxyUsd()).isEqualTo(2_000_000_000.0);
        assertThat(result.fy2FcfProxyUsd()).isNull();
        assertThat(result.impliedGrowthYears3To10Pct()).isNotNull();
    }

    @Test
    void calculate_unprofitableCompanyWithNegativeFcf_returnsNullGracefully() {
        LocalDate analysisDate = LocalDate.of(2026, 6, 1);
        EarningsEstimateDto fy1 = createEstimate(LocalDate.of(2026, 12, 31), 1_500_000_000.0, -1.20);
        EarningsEstimateDto fy2 = createEstimate(LocalDate.of(2027, 12, 31), 1_800_000_000.0, -0.80);
        List<CompanyFinancialRatiosDto> lossRatios = List.of(
                createRatio(2025, -0.15f, -200_000_000.0f, -250_000_000.0f, null)
        );

        ForwardReverseDcfRequest request = ForwardReverseDcfRequest.builder()
                .marketCap(8_000_000_000.0)
                .currentSharePrice(15.0f)
                .analysisDate(analysisDate)
                .fy1(fy1)
                .fy2(fy2)
                .annualRatios(lossRatios)
                .build();

        assertThat(calculator.calculate(request)).isNull();
    }

    @Test
    void calculate_acrossRepresentativeCompanyArchetypes_assignsExpectedRelativeZones() {
        LocalDate analysisDate = LocalDate.of(2026, 1, 1);
        LocalDate fy1Date = LocalDate.of(2026, 12, 31);
        LocalDate fy2Date = LocalDate.of(2027, 12, 31);

        // 1. Defensive value (KO/PG archetype): low market cap relative to steady FCF -> DEEP_DISCOUNT (< 3% required growth)
        ForwardReverseDcfDto valueResult = calculator.calculate(ForwardReverseDcfRequest.builder()
                .marketCap(140_000_000_000.0)
                .currentSharePrice(60.0f)
                .analysisDate(analysisDate)
                .fy1(createEstimate(fy1Date, 155_000_000_000.0, 2.85))
                .fy2(createEstimate(fy2Date, 162_000_000_000.0, 3.00))
                .annualRatios(List.of(createRatio(2025, 0.25f, 12_000_000_000.0f, 12_000_000_000.0f, 0.05f)))
                .ttmRatio(createRatio(2025, 0.25f, 12_000_000_000.0f, 12_000_000_000.0f, 0.05f))
                .build());
        assertThat(valueResult).isNotNull();
        assertThat(valueResult.relativeValuationZone()).isEqualTo(ValuationZone.DEEP_DISCOUNT);

        // 2. Cyclical / diverging proxy paths (CAT archetype): revenue path vs EPS path diverge > 30% -> flags divergence
        ForwardReverseDcfDto cyclicalResult = calculator.calculate(ForwardReverseDcfRequest.builder()
                .marketCap(160_000_000_000.0)
                .currentSharePrice(320.0f)
                .analysisDate(analysisDate)
                .fy1(createEstimate(fy1Date, 80_000_000_000.0, 20.0))
                .fy2(createEstimate(fy2Date, 95_000_000_000.0, 14.0))
                .annualRatios(List.of(createRatio(2025, 0.18f, 10_000_000_000.0f, 10_000_000_000.0f, 0.08f)))
                .build());
        assertThat(cyclicalResult).isNotNull();
        assertThat(cyclicalResult.flagFcfProxyDivergence()).isTrue();
    }

    @Test
    void calculate_nvdaHypergrowthWithQuarterlyMarginDrop_usesUnifiedZoneLegendAndBuildsMarginSensitivity() {
        LocalDate analysisDate = LocalDate.of(2026, 11, 4);
        EarningsEstimateDto fy1 = createEstimate(LocalDate.of(2027, 1, 31), 413_430_000_000.0, 9.31);
        EarningsEstimateDto fy2 = createEstimate(LocalDate.of(2028, 1, 31), 692_200_000_000.0, 15.91);

        List<CompanyFinancialRatiosDto> annual = List.of(
                createRatio(2026, 0.299f, 96_000_000_000.0f, 113_000_000_000.0f, 0.60f),
                createRatio(2025, 0.299f, 60_000_000_000.0f, 70_000_000_000.0f, 1.25f),
                createRatio(2024, 0.200f, 27_000_000_000.0f, 32_000_000_000.0f, 0.50f)
        );
        CompanyFinancialRatiosDto ttm = createRatio(2026, 0.299f, 96_580_000_000.0f, 113_000_000_000.0f, 0.34f);
        List<CompanyFinancialRatiosDto> quarterly = List.of(
                createRatio(2026, 0.222f, 22_000_000_000.0f, 31_000_000_000.0f, -0.05f)
        );

        ForwardReverseDcfDto result = calculator.calculate(ForwardReverseDcfRequest.builder()
                .marketCap(4_935_000_000_000.0)
                .currentSharePrice(202.06f)
                .analysisDate(analysisDate)
                .fy1(fy1)
                .fy2(fy2)
                .annualRatios(annual)
                .quarterlyRatios(quarterly)
                .ttmRatio(ttm)
                .build());

        assertThat(result).isNotNull();
        // 12.0% implied growth must be AGGRESSIVE under unified legend (never DEEP_DISCOUNT!)
        assertThat(result.impliedGrowthYears3To10Pct()).isCloseTo(12.0f, within(0.5f));
        assertThat(result.relativeValuationZone()).isEqualTo(ValuationZone.AGGRESSIVE);
        assertThat(result.marginSensitivity()).hasSize(3);
        assertThat(result.marginSensitivity().getFirst().scenarioLabel()).isEqualTo("Latest Quarter FCF Margin");
        assertThat(result.marginSensitivity().getFirst().assumedFcfMarginPct()).isEqualTo(22.2f);
        assertThat(result.marginSensitivity().getFirst().impliedGrowthYears3To10Pct()).isGreaterThan(16.0f);
        assertThat(result.marginSensitivity().getFirst().valuationZone()).isEqualTo(ValuationZone.AGGRESSIVE);
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
