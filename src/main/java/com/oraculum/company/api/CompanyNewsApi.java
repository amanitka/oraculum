package com.oraculum.company.api;

import com.oraculum.company.api.dto.NewsTickerDto;
import com.oraculum.company.api.dto.TickerNewsSentimentDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CompanyNewsApi {
    Optional<OffsetDateTime> getNewsLastTimePublished();
    List<NewsTickerDto> getNewsByTicker(String ticker, LocalDate after);
    Optional<TickerNewsSentimentDto> getNewsSentimentByTicker(String ticker);
    
    void createOrUpdateNewsBatch(List<com.oraculum.company.api.dto.NewsArticleDto> articles);
}
