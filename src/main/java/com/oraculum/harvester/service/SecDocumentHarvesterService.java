package com.oraculum.harvester.service;

import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.TickerDocumentSyncStatusDto;
import com.oraculum.company.api.dto.TickerKeyDto;
import com.oraculum.harvester.api.dto.FetchSecDocumentsRequest;
import com.oraculum.harvester.provider.SecCikMapperClient;
import com.oraculum.harvester.provider.SecEdgarIndexClient;
import com.oraculum.harvester.provider.dto.SecDailyFilingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecDocumentHarvesterService {

    private final CompanyTickerDocumentApi companyTickerDocumentApi;
    private final SecEdgarIndexClient secEdgarIndexClient;
    private final SecCikMapperClient secCikMapperClient;

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

        List<SecDailyFilingDto> filteredFilings = getFilteredDailyFilings(targetDate);
        if (filteredFilings.isEmpty()) return List.of();

        List<String> indexCiks = filteredFilings.stream().map(SecDailyFilingDto::cik).distinct().toList();
        List<String> targetedTickers = mapCiksToTrackedTickers(indexCiks);

        if (targetedTickers.isEmpty()) {
            log.info("None of the CIKs in today's index map to actively tracked tickers.");
            return List.of();
        }

        List<TickerDocumentSyncStatusDto> statuses = companyTickerDocumentApi.getSyncStatusesByTickers(targetedTickers);
        List<TickerDocumentSyncStatusDto> newStatuses = filterByNewFilings(statuses, filteredFilings);

        if (newStatuses.isEmpty()) {
            log.info("All filings for tracked companies are already processed.");
            return List.of();
        }

        log.info("Found {} new SEC document types to process.", newStatuses.size());
        return toBatchedRequests(buildTickerDocumentItems(newStatuses));
    }

    private List<FetchSecDocumentsRequest.TickerDocumentItem> buildTickerDocumentItems(
            List<TickerDocumentSyncStatusDto> statuses) {
        Map<TickerKey, List<TickerDocumentSyncStatusDto>> byTicker = statuses.stream()
                .collect(Collectors.groupingBy(dto -> new TickerKey(dto.getTicker(), dto.getMarket())));

        return byTicker.entrySet().stream()
                .map(this::buildItemForTicker)
                .toList();
    }

    private FetchSecDocumentsRequest.TickerDocumentItem buildItemForTicker(
            Map.Entry<TickerKey, List<TickerDocumentSyncStatusDto>> entry) {
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

    private List<SecDailyFilingDto> getFilteredDailyFilings(LocalDate targetDate) {
        List<SecDailyFilingDto> dailyFilings = secEdgarIndexClient.downloadDailyIndex(targetDate);
        if (dailyFilings.isEmpty()) {
            log.info("No filings found in daily index for {}", targetDate);
        }
        return dailyFilings;
    }

    private List<String> mapCiksToTrackedTickers(List<String> indexCiks) {
        Map<String, String> tickerToCikMap = secCikMapperClient.loadTickerToCikMapping();
        Map<String, List<String>> cikToTickers = new HashMap<>();
        for (Map.Entry<String, String> entry : tickerToCikMap.entrySet()) {
            cikToTickers.computeIfAbsent(entry.getValue(), _ -> new ArrayList<>()).add(entry.getKey());
        }

        return indexCiks.stream()
                .map(cikToTickers::get)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .distinct()
                .toList();
    }

    private List<TickerDocumentSyncStatusDto> filterByNewFilings(List<TickerDocumentSyncStatusDto> statuses, List<SecDailyFilingDto> filings) {
        Map<String, Map<TickerDocumentType, LocalDate>> maxDateByCikAndForm = filings.stream()
                .collect(Collectors.groupingBy(SecDailyFilingDto::cik,
                        Collectors.toMap(SecDailyFilingDto::formType, SecDailyFilingDto::dateFiled,
                                (d1, d2) -> d1.isAfter(d2) ? d1 : d2)));

        return statuses.stream()
                .filter(status -> isNewFiling(status, maxDateByCikAndForm))
                .toList();
    }

    private boolean isNewFiling(TickerDocumentSyncStatusDto status,
                                Map<String, Map<TickerDocumentType, LocalDate>> maxDateByCikAndForm) {
        String paddedCik = resolvePaddedCik(status);
        if (paddedCik == null) return false;

        Map<TickerDocumentType, LocalDate> formDates = maxDateByCikAndForm.get(paddedCik);
        if (formDates == null) return false;

        LocalDate dateFiled = formDates.get(status.getDocumentType());
        if (dateFiled == null) return false;

        LocalDate lastProcessed = status.getLastProcessedFileDate();
        return lastProcessed == null || !dateFiled.isBefore(lastProcessed);
    }

    private String resolvePaddedCik(TickerDocumentSyncStatusDto status) {
        String cik = status.getCik();
        if (cik == null || cik.isBlank()) {
            cik = secCikMapperClient.getCikForTicker(status.getTicker());
        }
        if (cik != null && !cik.isBlank()) {
            try {
                return String.format("%010d", Long.parseLong(cik));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private record TickerKey(String ticker, String market) {
    }
}
