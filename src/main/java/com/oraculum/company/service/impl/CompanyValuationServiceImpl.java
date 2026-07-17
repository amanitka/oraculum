package com.oraculum.company.service.impl;

import com.oraculum.company.api.CompanyFinancialDataApi;
import com.oraculum.company.api.CompanySharePriceApi;
import com.oraculum.company.api.CompanyValuationApi;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import com.oraculum.company.service.calculator.HistoricalValuationCalculator;
import com.oraculum.company.service.calculator.ReverseDcfCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyValuationServiceImpl implements CompanyValuationApi {

    private final CompanyFinancialDataApi companyFinancialDataApi;
    private final CompanySharePriceApi companySharePriceApi;

    private final ReverseDcfCalculator reverseDcfCalculator;
    private final HistoricalValuationCalculator historicalValuationCalculator;

    @Override
    @Transactional(readOnly = true)
    public ReverseDcfDto calculateReverseDcf(int companyId) {
        List<SharePriceSignalDto> dailySignals = companySharePriceApi.getDailySharePriceSignalsByCompanyId(
                companyId, LocalDate.now().minusDays(30));

        Map<StatementVariant, List<CompanyFinancialRatiosDto>> ratios = companyFinancialDataApi
                .getCompanyFinancialRatiosByCompanyId(companyId, LocalDate.now().minusYears(10))
                .stream()
                .collect(Collectors.groupingBy(CompanyFinancialRatiosDto::variant));

        return reverseDcfCalculator.calculate(dailySignals, ratios);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricalValuationSummaryDto> calculateHistoricalValuationPercentiles(int companyId) {
        List<SharePriceSignalDto> dailySignals = companySharePriceApi.getDailySharePriceSignalsByCompanyId(
                companyId, LocalDate.now().minusDays(30));

        List<SharePriceSignalDto> monthlySignals = companySharePriceApi.getMonthlySharePriceSignalsByCompanyId(
                companyId, LocalDate.now().minusYears(10));

        return historicalValuationCalculator.calculate(dailySignals, monthlySignals);
    }
}
