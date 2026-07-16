package com.oraculum.harvester.service;

import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.domain.TickerDocumentProvider;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.TickerDocumentSyncStatusDto;
import com.oraculum.harvester.api.dto.FetchSecDocumentsRequest;
import com.oraculum.company.api.dto.TickerKeyDto;
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

    public Optional<FetchSecDocumentsRequest> buildSecDocumentsRequest(List<TickerKeyDto> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Optional.empty();
        }

        List<CompanyDto> companies = resolveRequestedCompanies(tickers);

        if (companies.isEmpty()) {
            log.info("No matching companies found to refresh.");
            return Optional.empty();
        }

        Map<String, String> tickerToCik = companies.stream()
                .collect(Collectors.toMap(CompanyDto::ticker, c -> c.cik() != null ? c.cik() : "", (existing, _) -> existing));

        List<TickerKeyDto> usTickers = companies.stream()
                .map(c -> new TickerKeyDto(c.ticker(), c.market()))
                .toList();

        List<FetchSecDocumentsRequest.TickerDocumentItem> items = buildDocumentItems(usTickers, tickerToCik);
        return Optional.of(FetchSecDocumentsRequest.builder().items(items).build());
    }

    private List<CompanyDto> resolveRequestedCompanies(List<TickerKeyDto> requestedTickers) {
        if (requestedTickers == null || requestedTickers.isEmpty()) {
            return List.of();
        }

        Map<String, List<TickerKeyDto>> byMarket = requestedTickers.stream()
                .collect(Collectors.groupingBy(TickerKeyDto::market));

        List<CompanyDto> result = new ArrayList<>();
        for (Map.Entry<String, List<TickerKeyDto>> entry : byMarket.entrySet()) {
            String market = entry.getKey();
            List<String> tickersInMarket = entry.getValue().stream().map(TickerKeyDto::ticker).toList();
            result.addAll(companyMetadataApi.getCompaniesByMarketAndTickers(market, tickersInMarket));
        }
        return result;
    }

    private List<FetchSecDocumentsRequest.TickerDocumentItem> buildDocumentItems(List<TickerKeyDto> tickers, Map<String, String> tickerToCik) {
        List<String> tickerStrings = tickers.stream().map(TickerKeyDto::ticker).toList();
        Map<String, Map<TickerDocumentType, LocalDate>> statuses = fetchAndGroupStatuses(tickerStrings);
        List<TickerDocumentType> secDocTypes = getSecDocumentTypes();

        return tickers.stream()
                .map(t -> {
                    var docRequests = buildDocumentRequests(statuses.getOrDefault(t.ticker(), Map.of()), secDocTypes);
                    return FetchSecDocumentsRequest.TickerDocumentItem.builder()
                            .ticker(t.ticker())
                            .market(t.market())
                            .cik(tickerToCik.get(t.ticker()))
                            .documentTypes(docRequests)
                            .build();
                })
                .toList();
    }

    private List<FetchSecDocumentsRequest.DocumentTypeRequest> buildDocumentRequests(Map<TickerDocumentType, LocalDate> tickerStatuses,
                                                                                     List<TickerDocumentType> secDocTypes) {
        return secDocTypes.stream()
                .map(docType -> {
                    LocalDate lastDate = tickerStatuses.get(docType);
                    if (lastDate == null) {
                        lastDate = LocalDate.now().minusYears(1);
                    }
                    return FetchSecDocumentsRequest.DocumentTypeRequest.builder()
                            .documentType(docType.getCode())
                            .lastProcessedFileDate(lastDate)
                            .build();
                })
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

    public List<FetchSecDocumentsRequest> buildStaleSecDocumentsRequests() {
        log.info("Checking for stale SEC documents to refresh...");
        List<TickerDocumentSyncStatusDto> staleDocs = companyTickerDocumentApi.getStaleSecDocuments(100);
        if (staleDocs.isEmpty()) {
            log.info("No stale SEC documents found.");
            return List.of();
        }

        List<FetchSecDocumentsRequest.TickerDocumentItem> items = buildStaleDocumentItems(staleDocs);
        List<List<FetchSecDocumentsRequest.TickerDocumentItem>> batches = chunkList(items);

        return batches.stream()
                .map(batchItems -> FetchSecDocumentsRequest.builder().items(batchItems).build())
                .toList();
    }

    private <T> List<List<T>> chunkList(List<T> list) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += 20) {
            chunks.add(list.subList(i, Math.min(i + 20, list.size())));
        }
        return chunks;
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
        String cik = entry.getValue().stream()
                .map(TickerDocumentSyncStatusDto::getCik)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        List<FetchSecDocumentsRequest.DocumentTypeRequest> docRequests = entry.getValue().stream()
                .map(this::buildDocumentTypeRequest)
                .toList();

        return FetchSecDocumentsRequest.TickerDocumentItem.builder()
                .ticker(key.ticker())
                .market(key.market())
                .cik(cik != null && !cik.isBlank() ? cik : null)
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

    private record TickerKey(String ticker, String market) {
    }
}

