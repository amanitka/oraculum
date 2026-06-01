package com.oraculum.analyst.agents.tools;

import com.oraculum.analyst.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyDto;

import java.time.LocalDate;

public interface DataTools {
    CompanyDto getCompany(String ticker, String market);

    String getIncomeStatementHistory(int companyId, StatementVariant variant, int limit);

    String getBalanceSheetHistory(int companyId, StatementVariant variant, int limit);

    String getCashFlowHistory(int companyId, StatementVariant variant, int limit);

    String getSharePriceWindow(int companyId, LocalDate start, LocalDate end);

    String getSharePriceSignals(int companyId, LocalDate asOf);

    String getDerivedMetrics(int companyId, StatementVariant variant, int limit);

    String getRecentNews(String ticker, int daysBack, int limit);
}