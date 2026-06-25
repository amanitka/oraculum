package com.oraculum.company.service.impl;

import com.oraculum.company.api.CompanyFinancialDataApi;
import com.oraculum.company.api.dto.*;
import com.oraculum.company.domain.IndustryFinancialRatiosRepository;
import com.oraculum.company.repository.BalanceSheetRepository;
import com.oraculum.company.repository.CashFlowStatementRepository;
import com.oraculum.company.repository.CompanyFinancialRatiosRepository;
import com.oraculum.company.repository.IncomeStatementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyFinancialDataServiceImpl implements CompanyFinancialDataApi {

    private final BalanceSheetRepository balanceSheetRepository;
    private final CashFlowStatementRepository cashFlowStatementRepository;
    private final IncomeStatementRepository incomeStatementRepository;
    private final CompanyFinancialRatiosRepository companyFinancialRatiosRepository;
    private final IndustryFinancialRatiosRepository industryFinancialRatiosRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BalanceSheetDto> getBalanceSheetsByCompanyId(int companyId, LocalDate after) {
        return balanceSheetRepository.findByCompanyIdAndReportDateAfter(companyId, after)
                .stream()
                .map(BalanceSheetDto::fromEntity)
                .sorted(Comparator.comparing(BalanceSheetDto::fiscalYear, Comparator.reverseOrder()).thenComparing(BalanceSheetDto::fiscalPeriod, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashFlowStatementDto> getCashFlowStatementsByCompanyId(int companyId, LocalDate after) {
        return cashFlowStatementRepository.findByCompanyIdAndReportDateAfter(companyId, after)
                .stream()
                .map(CashFlowStatementDto::fromEntity)
                .sorted(Comparator.comparing(CashFlowStatementDto::fiscalYear, Comparator.reverseOrder()).thenComparing(CashFlowStatementDto::fiscalPeriod, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncomeStatementDto> getIncomeStatementsByCompanyId(int companyId, LocalDate after) {
        return incomeStatementRepository.findByCompanyIdAndReportDateAfter(companyId, after)
                .stream()
                .map(IncomeStatementDto::fromEntity)
                .sorted(Comparator.comparing(IncomeStatementDto::fiscalYear, Comparator.reverseOrder()).thenComparing(IncomeStatementDto::fiscalPeriod, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyFinancialRatiosDto> getCompanyFinancialRatiosByCompanyId(int companyId, LocalDate after) {
        return companyFinancialRatiosRepository.findByCompanyIdAndReportDateAfter(companyId, after)
                .stream()
                .map(CompanyFinancialRatiosDto::fromEntity)
                .sorted(Comparator.comparing(CompanyFinancialRatiosDto::reportDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndustryFinancialRatiosDto> getIndustryFinancialRatiosByIndustryName(String industryName) {
        return industryFinancialRatiosRepository.findByIndustryName(industryName).stream()
                .map(IndustryFinancialRatiosDto::fromEntity)
                .collect(Collectors.toList());
    }
}
