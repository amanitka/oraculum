package com.oraculum.harvester.api;

import com.oraculum.harvester.api.dto.CompanyNewsDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HarvesterLiveApi {
    /**
     * Fetches earnings estimates (EPS + revenue, annual + quarterly) for the given ticker.
     * Returns empty Optional if the daily API quota is exhausted.
     */
    Optional<List<EarningsEstimateDto>> fetchEarningsEstimates(String ticker);

    /**
     * Fetches recent company news from Finnhub for the given ticker and date range.
     * Articles are deduplicated, bot-filtered, and sorted from newest to oldest.
     */
    List<CompanyNewsDto> fetchCompanyNews(String ticker, LocalDate from, LocalDate to);

    /**
     * Fetches recent company news from Finnhub for the given ticker and date range,
     * optionally limiting the maximum number of returned articles.
     */
    default List<CompanyNewsDto> fetchCompanyNews(String ticker, LocalDate from, LocalDate to, int limit) {
        List<CompanyNewsDto> news = fetchCompanyNews(ticker, from, to);
        if (limit > 0 && news.size() > limit) {
            return news.subList(0, limit);
        }
        return news;
    }
}
