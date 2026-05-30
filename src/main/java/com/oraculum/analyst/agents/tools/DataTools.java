package com.oraculum.analyst.agents.tools;

import com.oraculum.analyst.domain.StatementTemplate;
import com.oraculum.analyst.domain.StatementVariant;
import com.oraculum.company.api.dto.TickerDto;

import java.time.LocalDate;

public interface DataTools {
    TickerDto getTickerProfile(String ticker);

    StatementTemplate resolveTemplate(String ticker);

    String getIncomeStatementHistory(String ticker, StatementTemplate template, StatementVariant variant, int limit);

    String getBalanceSheetHistory(String ticker, StatementTemplate template, StatementVariant variant, int limit);

    String getCashFlowHistory(String ticker, StatementTemplate template, StatementVariant variant, int limit);

    String getPriceWindow(String ticker, LocalDate start, LocalDate end);

    String getSharePriceSignals(String ticker, String market, LocalDate asOf);

    String getDerivedMetrics(String ticker, StatementTemplate template, StatementVariant variant, int limit);

    String getRecentNews(String ticker, int daysBack);
}
