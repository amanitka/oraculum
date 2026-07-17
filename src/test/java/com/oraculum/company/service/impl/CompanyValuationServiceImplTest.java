package com.oraculum.company.service.impl;

import com.oraculum.company.api.CompanyFinancialDataApi;
import com.oraculum.company.api.CompanySharePriceApi;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import com.oraculum.company.service.calculator.HistoricalValuationCalculator;
import com.oraculum.company.service.calculator.ReverseDcfCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CompanyValuationServiceImplTest {

    @Mock
    private CompanyFinancialDataApi companyFinancialDataApi;
    @Mock
    private CompanySharePriceApi companySharePriceApi;
    @Mock
    private ReverseDcfCalculator reverseDcfCalculator;
    @Mock
    private HistoricalValuationCalculator historicalValuationCalculator;

    @InjectMocks
    private CompanyValuationServiceImpl service;

    @Test
    void calculateReverseDcf_loadsDataAndInvokesCalculator() {
        List<SharePriceSignalDto> daily = List.of(mock(SharePriceSignalDto.class));
        CompanyFinancialRatiosDto ratio = mock(CompanyFinancialRatiosDto.class);
        when(ratio.variant()).thenReturn(StatementVariant.TTM);
        List<CompanyFinancialRatiosDto> ttmRatios = List.of(ratio);

        when(companySharePriceApi.getDailySharePriceSignalsByCompanyId(anyInt(), any(LocalDate.class))).thenReturn(daily);
        when(companyFinancialDataApi.getCompanyFinancialRatiosByCompanyId(anyInt(), any(LocalDate.class))).thenReturn(ttmRatios);

        ReverseDcfDto expectedResult = mock(ReverseDcfDto.class);
        when(reverseDcfCalculator.calculate(any(), any())).thenReturn(expectedResult);

        ReverseDcfDto result = service.calculateReverseDcf(1);

        assertThat(result).isSameAs(expectedResult);
        verify(reverseDcfCalculator).calculate(daily, Map.of(StatementVariant.TTM, ttmRatios));
    }

    @Test
    void calculateHistoricalValuationPercentiles_loadsDataAndInvokesCalculator() {
        List<SharePriceSignalDto> daily = List.of(mock(SharePriceSignalDto.class));
        List<SharePriceSignalDto> monthly = List.of(mock(SharePriceSignalDto.class));

        when(companySharePriceApi.getDailySharePriceSignalsByCompanyId(anyInt(), any(LocalDate.class))).thenReturn(daily);
        when(companySharePriceApi.getMonthlySharePriceSignalsByCompanyId(anyInt(), any(LocalDate.class))).thenReturn(monthly);

        List<HistoricalValuationSummaryDto> expectedSummaries = List.of(mock(HistoricalValuationSummaryDto.class));
        when(historicalValuationCalculator.calculate(any(), any())).thenReturn(expectedSummaries);

        List<HistoricalValuationSummaryDto> result = service.calculateHistoricalValuationPercentiles(1);

        assertThat(result).isSameAs(expectedSummaries);
        verify(historicalValuationCalculator).calculate(daily, monthly);
    }
}
