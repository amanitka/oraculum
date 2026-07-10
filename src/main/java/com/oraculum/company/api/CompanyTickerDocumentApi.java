package com.oraculum.company.api;

import com.oraculum.company.api.dto.TickerDocumentSyncStatusDto;

import java.util.List;

public interface CompanyTickerDocumentApi {

    /**
     * Get sync statuses for a specific list of tickers within a market.
     */
    List<TickerDocumentSyncStatusDto> getSyncStatusesByTickersAndMarket(List<String> tickers, String market);
}
