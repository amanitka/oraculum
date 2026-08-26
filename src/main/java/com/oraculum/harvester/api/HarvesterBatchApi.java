package com.oraculum.harvester.api;

import com.oraculum.company.api.dto.TickerKeyDto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;


public interface HarvesterBatchApi {

    void refreshMarket();

    void refreshIndustry();

    void refreshCompany();

    void refreshFundamentals();

    void refreshSecDocuments(List<TickerKeyDto> tickers);

    void refreshStaleSecDocuments();

    void refreshDailyNewSecDocuments(LocalDate targetDate);

    void refreshNews();

    void refreshMacroeconomic();

    void refreshInsiderTransactions();

    void refreshSharePrices(boolean incremental, LocalDate fromDate);

    default void refreshSharePrices() {
        refreshSharePrices(true, null);
    }

    /**
     * Triggers a quarterly SEC 13F bulk ZIP download for the given year and quarter.
     * Useful for manual triggers and historical backfill.
     */
    void refresh13FBulk(int year, int quarter);

    /**
     * Triggers a 13F bulk download for the most recently completed quarter.
     * Derives the period by going 2 months back from today, then snapping to the
     * last day of that quarter — safe to call on the 20th of Feb/May/Aug/Nov.
     */
    default void refresh13FBulk() {
        LocalDate ref       = LocalDate.now().minusMonths(2);
        int lastMonthOfQtr  = ((ref.getMonthValue() - 1) / 3 + 1) * 3;
        LocalDate periodEnd = YearMonth.of(ref.getYear(), lastMonthOfQtr).atEndOfMonth();
        refresh13FBulk(periodEnd.getYear(), (periodEnd.getMonthValue() - 1) / 3 + 1);
    }
}

