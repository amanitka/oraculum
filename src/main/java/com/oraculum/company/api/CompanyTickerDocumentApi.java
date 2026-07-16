package com.oraculum.company.api;

import com.oraculum.company.api.domain.TickerDocumentProcessingStatus;
import com.oraculum.company.api.dto.TickerDocumentDto;
import com.oraculum.company.api.dto.TickerDocumentPendingDto;
import com.oraculum.company.api.dto.TickerDocumentSyncStatusDto;
import com.oraculum.company.api.dto.TickerKeyDto;

import java.time.LocalDate;
import java.util.List;

public interface CompanyTickerDocumentApi {
    List<TickerDocumentSyncStatusDto> getSyncStatusesByTickersAndMarket(List<String> tickers, String market);

    List<TickerDocumentSyncStatusDto> getStaleSecDocuments(int limit);

    List<TickerDocumentPendingDto> getPendingRawDocuments(int limit, int maxPriority);

    void createDocumentSummary(TickerDocumentDto summary);

    void updateRawDocumentStatus(String id, LocalDate reportPeriod, TickerDocumentProcessingStatus status);

    List<TickerDocumentPendingDto> getPendingRawDocumentsByTicker(TickerKeyDto tickerKey, int maxPriority);

    List<TickerDocumentDto> getDocumentsByTicker(TickerKeyDto tickerKey);
}
