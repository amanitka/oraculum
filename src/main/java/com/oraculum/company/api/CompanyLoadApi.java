package com.oraculum.company.api;

import com.oraculum.company.api.dto.IndustryDto;
import com.oraculum.company.api.dto.MarketDto;
import com.oraculum.company.api.dto.NewsArticleDto;

import java.util.List;

/**
 * Public API for loading reference and market data into the Company module.
 * Consumed by the load domain's Kafka listeners.
 */
public interface CompanyLoadApi {

    void createOrUpdateMarket(MarketDto market);

    void createOrUpdateIndustry(IndustryDto industry);

    void createOrUpdateNewsBatch(List<NewsArticleDto> articles);
}
