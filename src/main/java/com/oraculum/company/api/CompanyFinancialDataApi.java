package com.oraculum.company.api;

import com.oraculum.company.api.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface CompanyFinancialDataApi {
    List<BalanceSheetDto> getBalanceSheetsByCompanyId(int companyId, LocalDate after);

    List<CashFlowStatementDto> getCashFlowStatementsByCompanyId(int companyId, LocalDate after);

    List<IncomeStatementDto> getIncomeStatementsByCompanyId(int companyId, LocalDate after);

    List<CompanyFinancialRatiosDto> getCompanyFinancialRatiosByCompanyId(int companyId, LocalDate after);

    List<IndustryFinancialRatiosDto> getIndustryFinancialRatiosByIndustryName(String industryName);
}
