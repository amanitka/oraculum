package com.oraculum.harvester.provider.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.company.api.dto.NewsArticleDto;

import java.util.List;

public record AlphaVantageNewsResponse(
        String items,
        @JsonProperty("sentiment_score_definition") String sentimentScoreDefinition,
        @JsonProperty("relevance_score_definition") String relevanceScoreDefinition,
        List<NewsArticleDto> feed
) {}
