package com.oraculum.analyst.agents.tools;

import com.oraculum.analyst.domain.IncomeStatementTemplate;
import com.oraculum.analyst.domain.StatementVariant;
import com.oraculum.company.api.dto.TickerDto;

import java.time.LocalDate;

public interface DataTools {
    TickerDto getTickerProfile(String ticker);
    IncomeStatementTemplate resolveTemplate(String ticker);
    String getIncomeStatementHistory(String ticker, IncomeStatementTemplate template, StatementVariant variant, int limit);
    String getBalanceSheetHistory(String ticker, IncomeStatementTemplate template, StatementVariant variant, int limit);
    String getCashFlowHistory(String ticker, IncomeStatementTemplate template, StatementVariant variant, int limit);
    String getPriceWindow(String ticker, LocalDate start, LocalDate end);
    String getSharePriceSignals(String ticker, String market, LocalDate asOf);
    String getDerivedMetrics(String ticker, IncomeStatementTemplate template, StatementVariant variant, int limit);
    String getRecentNews(String ticker, int daysBack);
}
