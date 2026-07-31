package com.oraculum.harvester.provider;

import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.harvester.provider.dto.SecDailyFilingDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
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

    public Map<String, Map<TickerDocumentType, SecDailyFilingDto>> downloadDailyIndex(LocalDate targetDate) {
        try {
            return fetchDailyIndexForDate(targetDate);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.FORBIDDEN) || e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                log.warn("Daily index for {} is not yet available on SEC servers. Falling back to previous day.", targetDate);
                LocalDate fallbackDate = targetDate.minusDays(1);
                try {
                    return fetchDailyIndexForDate(fallbackDate);
                } catch (HttpClientErrorException ex) {
                    if (ex.getStatusCode().isSameCodeAs(HttpStatus.FORBIDDEN) || ex.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                        log.warn("Daily index for {} is also not available.", fallbackDate);
                        return Map.of();
                    }
                    throw ex;
                }
            }
            throw e;
        }
    }

    private Map<String, Map<TickerDocumentType, SecDailyFilingDto>> fetchDailyIndexForDate(LocalDate date) {
        String uri = buildDailyIndexUri(date);
        log.info("Downloading SEC Daily Master Index for date: {} (relative path: {})", date, uri);

        try {
            return restClient.get()
                    .uri(uri)
                    .exchange((_, response) -> {
                        String body = handleExchangeResponse(uri, response);
                        if (body == null || body.isBlank()) {
                            return Map.of();
                        }
                        return parseIndexFile(body);
                    });
        } catch (HttpClientErrorException e) {
            throw e; // Rethrow to let the caller handle the fallback
        } catch (RestClientException e) {
            log.error("Failed to execute request to SEC EDGAR for relative path {}: {}", uri, e.getMessage());
            return Map.of();
        }
    }

    private String handleExchangeResponse(String uri, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        if (status.isSameCodeAs(HttpStatus.FORBIDDEN) ||
                status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
            throw new HttpClientErrorException(status);
        }
        if (status.isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE)) {
            log.warn("SEC EDGAR is temporarily unavailable (503). Maintenance in progress.");
            return null;
        }
        if (status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
            log.warn("Rate limited by SEC EDGAR (429). Try again later.");
            return null;
        }
        if (status.isError()) {
            log.error("Failed to download SEC daily index from relative path {}: {}", uri, status);
            return null;
        }
        return response.bodyTo(String.class);
    }

    private Map<String, Map<TickerDocumentType, SecDailyFilingDto>> parseIndexFile(String content) {
        Map<String, Map<TickerDocumentType, SecDailyFilingDto>> filingsByCik = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line).ifPresent(dto -> filingsByCik.computeIfAbsent(dto.cik(), _ -> new HashMap<>())
                        .merge(dto.formType(), dto, (d1, d2) -> d1.dateFiled().isAfter(d2.dateFiled()) ? d1 : d2));
            }
        } catch (Exception e) {
            log.error("Error parsing SEC master index file", e);
        }
        return filingsByCik;
    }

    private Optional<SecDailyFilingDto> parseLine(String line) {
        String[] parts = line.split("\\|");
        if (parts.length != 5 || line.startsWith("CIK")) {
            return Optional.empty();
        }

        try {
            String rawFormType = parts[2].trim();

            Optional<TickerDocumentType> optFormType = TickerDocumentType.fromSecFormName(rawFormType);
            if (optFormType.isEmpty()) {
                return Optional.empty();
            }

            String cik = parts[0].trim();
            String paddedCik = String.format("%010d", Long.parseLong(cik));

            String companyName = parts[1].trim();
            LocalDate dateFiled = LocalDate.parse(parts[3].trim(), DateTimeFormatter.BASIC_ISO_DATE);
            String filename = parts[4].trim();

            return Optional.of(new SecDailyFilingDto(paddedCik, companyName, optFormType.get(), dateFiled, filename));
        } catch (DateTimeParseException | NumberFormatException e) {
            log.debug("Skipping invalid line in master index: {}", line);
            return Optional.empty();
        }
    }
}
