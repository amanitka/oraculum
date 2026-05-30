package com.oraculum.analyst.agents.tools;

import com.oraculum.analyst.domain.StatementVariant;
import com.oraculum.company.api.dto.TickerDto;

import java.time.LocalDate;

public interface DataTools {
    TickerDto getTicker(String ticker, String market);

    String getIncomeStatementHistory(String ticker, StatementVariant variant, int limit);

    String getBalanceSheetHistory(String ticker, StatementVariant variant, int limit);

    String getCashFlowHistory(String ticker, StatementVariant variant, int limit);

    String getSharePriceWindow(String ticker, LocalDate start, LocalDate end);

    String getSharePriceSignals(String ticker, String market, LocalDate asOf);

    String getDerivedMetrics(String ticker, StatementVariant variant, int limit);

    String getRecentNews(String ticker, int daysBack, int limit);
}