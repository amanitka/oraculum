package com.oraculum.harvester.provider;

import com.oraculum.harvester.provider.dto.SecDailyFilingDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import com.oraculum.company.api.domain.TickerDocumentType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class SecEdgarIndexClient {

    private final RestClient restClient;

    public SecEdgarIndexClient(@Qualifier("secEdgarRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    private String buildDailyIndexUri(LocalDate targetDate) {
        int quarter = (targetDate.getMonthValue() - 1) / 3 + 1;
        String dateStr = targetDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        return String.format("/Archives/edgar/daily-index/%d/QTR%d/master.%s.idx", targetDate.getYear(), quarter, dateStr);
    }

    public List<SecDailyFilingDto> downloadDailyIndex(LocalDate targetDate) {
        try {
            return fetchDailyIndexForDate(targetDate);
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.NotFound e) {
            log.warn("Daily index for {} is not yet available on SEC servers. Falling back to previous day.", targetDate);
            LocalDate fallbackDate = targetDate.minusDays(1);
            try {
                return fetchDailyIndexForDate(fallbackDate);
            } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.NotFound ex) {
                log.warn("Daily index for {} is also not available.", fallbackDate);
                return List.of();
            }
        }
    }

    private List<SecDailyFilingDto> fetchDailyIndexForDate(LocalDate date) {
        String uri = buildDailyIndexUri(date);
        log.info("Downloading SEC Daily Master Index for date: {} (relative path: {})", date, uri);
        
        try {
            String response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                log.warn("Received empty response for SEC daily index at relative path {}", uri);
                return List.of();
            }

            return parseIndexFile(response);
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.NotFound e) {
            throw e; // Rethrow to let the caller handle the fallback
        } catch (RestClientException e) {
            log.error("Failed to download SEC daily index from relative path {}", uri, e);
            return List.of();
        }
    }

    private List<SecDailyFilingDto> parseIndexFile(String content) {
        List<SecDailyFilingDto> filings = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line).ifPresent(filings::add);
            }
        } catch (Exception e) {
            log.error("Error parsing SEC master index file", e);
        }
        return filings;
    }

    private Optional<SecDailyFilingDto> parseLine(String line) {
        String[] parts = line.split("\\|");
        if (parts.length != 5 || line.startsWith("CIK")) {
            return Optional.empty();
        }

        try {
            String cik = parts[0].trim();
            String companyName = parts[1].trim();
            String rawFormType = parts[2].trim();
            
            Optional<TickerDocumentType> optFormType = TickerDocumentType.fromSecFormName(rawFormType);
            if (optFormType.isEmpty()) {
                return Optional.empty();
            }

            LocalDate dateFiled = LocalDate.parse(parts[3].trim(), DateTimeFormatter.BASIC_ISO_DATE);
            String filename = parts[4].trim();

            String paddedCik = String.format("%010d", Long.parseLong(cik));
            return Optional.of(new SecDailyFilingDto(paddedCik, companyName, optFormType.get(), dateFiled, filename));
        } catch (DateTimeParseException | NumberFormatException e) {
            log.debug("Skipping invalid line in master index: {}", line);
            return Optional.empty();
        }
    }
}
