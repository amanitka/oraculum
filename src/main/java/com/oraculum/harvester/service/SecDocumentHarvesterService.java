package com.oraculum.harvester.service;

import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.TickerDocumentSyncStatusDto;
import com.oraculum.company.api.dto.TickerKeyDto;
import com.oraculum.harvester.api.dto.FetchSecDocumentsRequest;
import com.oraculum.harvester.provider.SecEdgarIndexClient;
import com.oraculum.harvester.provider.dto.SecDailyFilingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecDocumentHarvesterService {

    private final CompanyTickerDocumentApi companyTickerDocumentApi;
    private final SecEdgarIndexClient secEdgarIndexClient;

    public Optional<FetchSecDocumentsRequest> buildSecDocumentsRequest(List<TickerKeyDto> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Optional.empty();
        }

        List<String> tickerStrings = tickers.stream().map(TickerKeyDto::ticker).distinct().toList();
        List<TickerDocumentSyncStatusDto> statuses = companyTickerDocumentApi.getSyncStatusesByTickers(tickerStrings);

        if (statuses.isEmpty()) {
            log.info("No matching companies found to refresh.");
            return Optional.empty();
        }

        List<FetchSecDocumentsRequest.TickerDocumentItem> items = buildTickerDocumentItems(statuses);
        return Optional.of(FetchSecDocumentsRequest.builder().items(items).build());
    }

    public List<FetchSecDocumentsRequest> buildStaleSecDocumentsRequests() {
        log.info("Checking for stale SEC documents to refresh...");
        List<TickerDocumentSyncStatusDto> staleDocs = companyTickerDocumentApi.getStaleSecDocuments(100);
        if (staleDocs.isEmpty()) {
            log.info("No stale SEC documents found.");
            return List.of();
        }

        return toBatchedRequests(buildTickerDocumentItems(staleDocs));
    }

    public List<FetchSecDocumentsRequest> buildDailyNewSecDocumentsRequests(LocalDate targetDate) {
        log.info("Checking for new SEC documents in daily index for date: {}", targetDate);

        Map<String, Map<TickerDocumentType, SecDailyFilingDto>> filingsByCik = secEdgarIndexClient.downloadDailyIndex(targetDate);
        if (filingsByCik.isEmpty()) {
            log.info("No relevant filings found in daily index for {}", targetDate);
            return List.of();
        }

        List<String> targetedCiks = new ArrayList<>(filingsByCik.keySet());
        List<TickerDocumentSyncStatusDto> allStatuses = companyTickerDocumentApi.getSyncStatusesByCiks(targetedCiks);

        List<TickerDocumentSyncStatusDto> newStatuses = filterByNewFilings(allStatuses, filingsByCik);

        if (newStatuses.isEmpty()) {
            log.info("All filings for tracked companies are already processed.");
            return List.of();
        }

        log.info("Found {} new SEC document types to process.", newStatuses.size());
        return toBatchedRequests(buildTickerDocumentItems(newStatuses));
    }

    private List<FetchSecDocumentsRequest.TickerDocumentItem> buildTickerDocumentItems(List<TickerDocumentSyncStatusDto> statuses) {
        Map<TickerKey, List<TickerDocumentSyncStatusDto>> byTicker = statuses.stream()
                .collect(Collectors.groupingBy(dto -> new TickerKey(dto.getTicker(), dto.getMarket())));

        return byTicker.entrySet().stream()
                .map(this::buildItemForTicker)
                .toList();
    }

    private FetchSecDocumentsRequest.TickerDocumentItem buildItemForTicker(Map.Entry<TickerKey, List<TickerDocumentSyncStatusDto>> entry) {
        TickerKey key = entry.getKey();
        String cik = entry.getValue().stream()
                .map(TickerDocumentSyncStatusDto::getCik)
                .filter(c -> c != null && !c.isBlank())
                .findFirst().orElse(null);

        List<FetchSecDocumentsRequest.DocumentTypeRequest> docRequests = entry.getValue().stream()
                .map(this::buildDocumentTypeRequest)
                .toList();

        return FetchSecDocumentsRequest.TickerDocumentItem.builder()
                .ticker(key.ticker())
                .market(key.market())
                .cik(cik)
                .documentTypes(docRequests)
                .build();
    }

    private FetchSecDocumentsRequest.DocumentTypeRequest buildDocumentTypeRequest(TickerDocumentSyncStatusDto dto) {
        LocalDate lastDate = dto.getLastProcessedFileDate();
        if (lastDate == null) {
            lastDate = LocalDate.now().minusYears(1);
        }
        return FetchSecDocumentsRequest.DocumentTypeRequest.builder()
                .documentType(dto.getDocumentType().getCode())
                .lastProcessedFileDate(lastDate)
                .build();
    }

    private List<FetchSecDocumentsRequest> toBatchedRequests(
            List<FetchSecDocumentsRequest.TickerDocumentItem> items) {
        return chunkList(items).stream()
                .map(batch -> FetchSecDocumentsRequest.builder().items(batch).build())
                .toList();
    }

    private <T> List<List<T>> chunkList(List<T> list) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += 20) {
            chunks.add(list.subList(i, Math.min(i + 20, list.size())));
        }
        return chunks;
    }

    private List<TickerDocumentSyncStatusDto> filterByNewFilings(
            List<TickerDocumentSyncStatusDto> statuses,
            Map<String, Map<TickerDocumentType, SecDailyFilingDto>> filingsByCik) {

        return statuses.stream()
                .filter(status -> isNewFiling(status, filingsByCik))
                .toList();
    }

    private boolean isNewFiling(TickerDocumentSyncStatusDto status,
                                Map<String, Map<TickerDocumentType, SecDailyFilingDto>> filingsByCik) {
        String paddedCik = status.getCik();
        if (paddedCik == null || paddedCik.isBlank()) {
            return false;
        }

        try {
            paddedCik = String.format("%010d", Long.parseLong(paddedCik));
        } catch (NumberFormatException e) {
            return false;
        }

        Map<TickerDocumentType, SecDailyFilingDto> formFilings = filingsByCik.get(paddedCik);
        if (formFilings == null) return false;

        SecDailyFilingDto filing = formFilings.get(status.getDocumentType());
        if (filing == null) return false;

        LocalDate dateFiled = filing.dateFiled();
        LocalDate lastProcessed = status.getLastProcessedFileDate();

        return lastProcessed == null || !dateFiled.isBefore(lastProcessed);
    }

    private record TickerKey(String ticker, String market) {
    }
}
