package com.oraculum.company.api;

import com.oraculum.company.api.dto.TickerDocumentDto;
import com.oraculum.company.api.dto.TickerDocumentPendingDto;
import com.oraculum.company.api.dto.TickerDocumentSyncStatusDto;

import java.time.LocalDate;
import java.util.List;

public interface CompanyTickerDocumentApi {
    List<TickerDocumentSyncStatusDto> getSyncStatusesByTickersAndMarket(List<String> tickers, String market);

    List<TickerDocumentSyncStatusDto> getStaleSecDocuments(int limit);

    List<TickerDocumentPendingDto> getPendingRawDocuments(int limit);

    void createDocumentSummary(TickerDocumentDto summary);

    void updateRawDocumentStatus(String id, LocalDate reportPeriod, String status);

    List<TickerDocumentPendingDto> getPendingRawDocumentsByTicker(String ticker, String market);
}
