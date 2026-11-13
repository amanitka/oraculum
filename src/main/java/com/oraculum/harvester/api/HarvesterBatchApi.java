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
     * Triggers a 13F bulk download for the latest SEC-published quarter.
     * Since March 2024, SEC bulk datasets cover 3-month filing windows ending Feb/May/Aug/Nov
     * and are published at the start of Mar/Jun/Sep/Dec.
     */
    default void refresh13FBulk() {
        LocalDate periodEnd = resolveLatestAvailable13FPeriodEnd(LocalDate.now());
        refresh13FBulk(periodEnd.getYear(), quarterOf(periodEnd));
    }

    /**
     * Resolves the quarter-end date of the most recently published SEC 13F bulk dataset
     * as of the given date.
     */
    static LocalDate resolveLatestAvailable13FPeriodEnd(LocalDate asOfDate) {
        LocalDate ref = asOfDate.minusMonths(5);
        int lastMonthOfQtr = quarterOf(ref) * 3;
        return YearMonth.of(ref.getYear(), lastMonthOfQtr).atEndOfMonth();
    }

    private static int quarterOf(LocalDate date) {
        return (date.getMonthValue() - 1) / 3 + 1;
    }

    /**
     * Resolves distinct CUSIPs from SEC 13F holdings via OpenFIGI and updates t_company.
     */
    void refreshCompanyCusips();
}
