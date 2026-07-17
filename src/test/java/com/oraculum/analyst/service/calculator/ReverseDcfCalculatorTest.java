package com.oraculum.analyst.service.calculator;

import com.oraculum.analyst.dto.ReverseDcfResult;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReverseDcfCalculatorTest {

    private final ReverseDcfCalculator calculator = new ReverseDcfCalculator();

    @Test
    void calculate_returnsNullForInvalidInputs() {
        assertThat(calculator.calculate(null, Map.of())).isNull();
        assertThat(calculator.calculate(List.of(), Map.of())).isNull();
    }

    @Test
    void calculate_returnsValidImpliedGrowth() {
        SharePriceSignalDto latestDaily = mock(SharePriceSignalDto.class);
        when(latestDaily.marketCapitalization()).thenReturn(10_000_000_000f);

        CompanyFinancialRatiosDto ttmRatio = mock(CompanyFinancialRatiosDto.class);
        when(ttmRatio.freeCashFlow()).thenReturn(500_000_000f);

        Map<StatementVariant, List<CompanyFinancialRatiosDto>> ratios = Map.of(
                StatementVariant.TTM, List.of(ttmRatio),
                StatementVariant.ANNUAL, List.of()
        );

        ReverseDcfResult result = calculator.calculate(List.of(latestDaily), ratios);

        assertThat(result).isNotNull();
        assertThat(result.currentMarketCap()).isEqualTo(10_000_000_000f);
        assertThat(result.currentFcf()).isEqualTo(500_000_000f);
        assertThat(result.fcfYieldPct()).isEqualTo(5.0f);
        assertThat(result.discountRatePct()).isEqualTo(10.0f);
        assertThat(result.projectionYears()).isEqualTo(10);
        assertThat(result.terminalGrowthRatePct()).isEqualTo(3.0f);
        
        assertThat(result.impliedFcfGrowthRatePct()).isNotNull();
        assertThat(result.impliedFcfGrowthRatePct()).isBetween(-50f, 80f);
        assertThat(result.interpretation()).contains("At the current market capitalization of $10.00B");
        assertThat(result.interpretation()).contains("No historical FCF CAGR comparison available");
    }

    @Test
    void calculate_computesHistoricalCagrCorrectly() {
        SharePriceSignalDto latestDaily = mock(SharePriceSignalDto.class);
        when(latestDaily.marketCapitalization()).thenReturn(10_000_000_000f);

        CompanyFinancialRatiosDto ttmRatio = mock(CompanyFinancialRatiosDto.class);
        when(ttmRatio.freeCashFlow()).thenReturn(500_000_000f);

        CompanyFinancialRatiosDto ratio1 = mock(CompanyFinancialRatiosDto.class);
        when(ratio1.fiscalYear()).thenReturn(2020);
        when(ratio1.freeCashFlow()).thenReturn(100_000_000f);

        CompanyFinancialRatiosDto ratio2 = mock(CompanyFinancialRatiosDto.class);
        when(ratio2.fiscalYear()).thenReturn(2025);
        when(ratio2.freeCashFlow()).thenReturn(248_832_000f);

        Map<StatementVariant, List<CompanyFinancialRatiosDto>> ratios = Map.of(
                StatementVariant.TTM, List.of(ttmRatio),
                StatementVariant.ANNUAL, List.of(ratio2, ratio1)
        );

        ReverseDcfResult result = calculator.calculate(List.of(latestDaily), ratios);

        assertThat(result).isNotNull();
        assertThat(result.historicalFcfCagrPct()).isCloseTo(20.0f, org.assertj.core.api.Assertions.within(0.1f));
        assertThat(result.interpretation()).contains("For comparison, the company's historical annual FCF growth (CAGR) was 20.0%");
    }
}
