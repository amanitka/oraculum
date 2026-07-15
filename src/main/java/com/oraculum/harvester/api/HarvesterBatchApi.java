package com.oraculum.harvester.api;

import com.oraculum.company.api.dto.TickerKeyDto;

import java.time.LocalDate;
import java.util.List;

public interface HarvesterBatchApi {

    void refreshMarket();

    void refreshIndustry();

    void refreshCompany();

    void refreshFundamentals();

    void refreshSecDocuments(List<TickerKeyDto> tickers);

    void refreshStaleSecDocuments();

    void refreshNews();

    void refreshMacroeconomic();

    void refreshInsiderTransactions();

    void refreshSharePrices(boolean incremental, LocalDate fromDate);

    default void refreshSharePrices() {
        refreshSharePrices(true, null);
    }
}
