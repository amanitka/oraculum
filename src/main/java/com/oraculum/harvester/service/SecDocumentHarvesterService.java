package com.oraculum.harvester.service;

import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.domain.TickerDocumentProvider;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.TickerDocumentSyncStatusDto;
import com.oraculum.harvester.api.dto.FetchSecDocumentsRequest;
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

    private final CompanyMetadataApi companyMetadataApi;
    private final CompanyTickerDocumentApi companyTickerDocumentApi;

    public Optional<FetchSecDocumentsRequest> buildSecDocumentsRequest(List<String> tickers) {
        List<String> usTickers = resolveUsTickers(tickers);

        if (usTickers.isEmpty()) {
            log.info("No US tickers found to refresh.");
            return Optional.empty();
        }

        List<FetchSecDocumentsRequest.TickerDocumentItem> items = buildDocumentItems(usTickers);
        return Optional.of(FetchSecDocumentsRequest.builder().items(items).build());
    }

    private List<String> resolveUsTickers(List<String> requestedTickers) {
        List<String> usTickers = companyMetadataApi.getAllCompanies().stream()
                .filter(c -> c.market() != null && "US".equalsIgnoreCase(c.market().trim()))
                .map(CompanyDto::ticker)
                .toList();

        if (requestedTickers != null && !requestedTickers.isEmpty()) {
            Set<String> requestedUpper = requestedTickers.stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
            return usTickers.stream().filter(requestedUpper::contains).toList();
        }
        return usTickers;
    }

    private List<FetchSecDocumentsRequest.TickerDocumentItem> buildDocumentItems(List<String> tickers) {
        Map<String, Map<TickerDocumentType, LocalDate>> statuses = fetchAndGroupStatuses(tickers);
        List<TickerDocumentType> secDocTypes = getSecDocumentTypes();

        return tickers.stream()
                .map(ticker -> buildSingleItem(ticker, statuses.getOrDefault(ticker, Map.of()), secDocTypes))
                .toList();
    }

    private Map<String, Map<TickerDocumentType, LocalDate>> fetchAndGroupStatuses(List<String> tickers) {
        return companyTickerDocumentApi.getSyncStatusesByTickersAndMarket(tickers, "US").stream()
                .collect(Collectors.groupingBy(
                        TickerDocumentSyncStatusDto::getTicker,
                        Collectors.toMap(
                                TickerDocumentSyncStatusDto::getDocumentType,
                                dto -> dto.getLastProcessedFileDate() != null ? dto.getLastProcessedFileDate() : null,
                                (existing, _) -> existing
                        )
                ));
    }

    private List<TickerDocumentType> getSecDocumentTypes() {
        return Arrays.stream(TickerDocumentType.values())
                .filter(type -> TickerDocumentProvider.SEC == type.getProvider())
                .toList();
    }

    private FetchSecDocumentsRequest.TickerDocumentItem buildSingleItem(
            String ticker,
            Map<TickerDocumentType, LocalDate> tickerStatuses,
            List<TickerDocumentType> secDocTypes) {

        List<FetchSecDocumentsRequest.DocumentTypeRequest> docRequests = secDocTypes.stream()
                .map(docType -> FetchSecDocumentsRequest.DocumentTypeRequest.builder()
                        .documentType(docType.getCode())
                        .lastProcessedFileDate(tickerStatuses.get(docType))
                        .build())
                .toList();

        return FetchSecDocumentsRequest.TickerDocumentItem.builder()
                .ticker(ticker)
                .market("US")
                .documentTypes(docRequests)
                .build();
    }

    private record TickerKey(String ticker, String market) {}

    public Optional<FetchSecDocumentsRequest> buildStaleSecDocumentsRequest() {
        log.info("Checking for stale SEC documents to refresh...");
        List<TickerDocumentSyncStatusDto> staleDocs = companyTickerDocumentApi.getStaleSecDocuments(200);
        if (staleDocs.isEmpty()) {
            log.info("No stale SEC documents found.");
            return Optional.empty();
        }

        List<FetchSecDocumentsRequest.TickerDocumentItem> items = buildStaleDocumentItems(staleDocs);
        return Optional.of(FetchSecDocumentsRequest.builder().items(items).build());
    }

    private List<FetchSecDocumentsRequest.TickerDocumentItem> buildStaleDocumentItems(
            List<TickerDocumentSyncStatusDto> staleDocs) {
        Map<TickerKey, List<TickerDocumentSyncStatusDto>> byTicker = staleDocs.stream()
                .collect(Collectors.groupingBy(dto -> new TickerKey(dto.getTicker(), dto.getMarket())));

        return byTicker.entrySet().stream()
                .map(this::buildStaleItemForTicker)
                .toList();
    }

    private FetchSecDocumentsRequest.TickerDocumentItem buildStaleItemForTicker(
            Map.Entry<TickerKey, List<TickerDocumentSyncStatusDto>> entry) {
        TickerKey key = entry.getKey();
        List<FetchSecDocumentsRequest.DocumentTypeRequest> docRequests = entry.getValue().stream()
                .map(this::buildDocumentTypeRequest)
                .toList();

        return FetchSecDocumentsRequest.TickerDocumentItem.builder()
                .ticker(key.ticker())
                .market(key.market())
                .documentTypes(docRequests)
                .build();
    }

    private FetchSecDocumentsRequest.DocumentTypeRequest buildDocumentTypeRequest(TickerDocumentSyncStatusDto dto) {
        return FetchSecDocumentsRequest.DocumentTypeRequest.builder()
                .documentType(dto.getDocumentType().getCode())
                .lastProcessedFileDate(dto.getLastProcessedFileDate())
                .build();
    }
}

