package com.oraculum.harvester.api;

import java.time.LocalDate;
import java.util.List;

public interface HarvesterBatchApi {

    void refreshMarket();

    void refreshIndustry();

    void refreshCompany();

    void refreshFundamentals();

    void refreshUsTickerSecDocuments(List<String> tickers);

    void refreshNews();

    void refreshMacroeconomic();

    void refreshInsiderTransactions();

    void refreshSharePrices(boolean incremental, LocalDate fromDate);

    default void refreshSharePrices() {
        refreshSharePrices(true, null);
    }
}
