package com.oraculum.company.service.calculator;

import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HistoricalValuationCalculatorTest {

    private final HistoricalValuationCalculator calculator = new HistoricalValuationCalculator();

    @Test
    void calculate_returnsEmptyForNullOrEmptyInputs() {
        assertThat(calculator.calculate(null, List.of())).isEmpty();
        assertThat(calculator.calculate(List.of(), null)).isEmpty();
    }

    @Test
    void calculate_computesRangesCorrectly() {
        SharePriceSignalDto latestDaily = mock(SharePriceSignalDto.class);
        when(latestDaily.tradeDate()).thenReturn(LocalDate.of(2026, 7, 17));
        when(latestDaily.peRatio()).thenReturn(20.0f);
        when(latestDaily.enterpriseValueToEbitda()).thenReturn(15.0f);
        when(latestDaily.fcfYield()).thenReturn(5.0f);
        when(latestDaily.priceToSales()).thenReturn(3.0f);

        SharePriceSignalDto m1 = mock(SharePriceSignalDto.class);
        when(m1.tradeDate()).thenReturn(LocalDate.of(2026, 6, 1));
        when(m1.peRatio()).thenReturn(10.0f);
        when(m1.enterpriseValueToEbitda()).thenReturn(8.0f);
        when(m1.fcfYield()).thenReturn(8.0f);
        when(m1.priceToSales()).thenReturn(1.5f);

        SharePriceSignalDto m2 = mock(SharePriceSignalDto.class);
        when(m2.tradeDate()).thenReturn(LocalDate.of(2025, 6, 1));
        when(m2.peRatio()).thenReturn(30.0f);
        when(m2.enterpriseValueToEbitda()).thenReturn(22.0f);
        when(m2.fcfYield()).thenReturn(2.0f);
        when(m2.priceToSales()).thenReturn(4.5f);

        List<HistoricalValuationSummaryDto> results = calculator.calculate(List.of(latestDaily), List.of(m1, m2));

        assertThat(results).hasSize(4);

        HistoricalValuationSummaryDto pe = results.stream().filter(r -> r.metric().equals("P/E")).findFirst().orElseThrow();
        assertThat(pe.current()).isEqualTo(20.0f);
        assertThat(pe.avg10y()).isEqualTo(20.0f);
        assertThat(pe.percentile10y()).isEqualTo(50);
        assertThat(pe.min10y()).isEqualTo(10.0f);
        assertThat(pe.max10y()).isEqualTo(30.0f);
    }
}
