package com.oraculum.company.service.mapper;

import com.oraculum.company.api.dto.NewsArticleDto;
import com.oraculum.company.domain.NewsEntity;
import com.oraculum.company.domain.NewsTickerEntity;
import com.oraculum.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NewsArticleMapper {

    public NewsEntity toNewsEntity(NewsArticleDto dto) {
        NewsEntity entity = new NewsEntity();
        entity.setId(dto.id());
        entity.setTitle(dto.title());
        entity.setUrl(dto.url());
        entity.setTimePublished(DateTimeUtil.toOffsetDateTime(dto.timePublished()));
        entity.setAuthors(dto.authors());
        entity.setSummary(dto.summary());
        entity.setSource(dto.source());
        entity.setCategoryWithinSource(dto.categoryWithinSource());
        entity.setSourceDomain(dto.sourceDomain());
        entity.setTopics(dto.topics());
        entity.setOverallSentimentScore(dto.overallSentimentScore());
        entity.setOverallSentimentLabel(dto.overallSentimentLabel());
        entity.setExtractedAt(dto.extractedAt());
        entity.setSentimentScoreDefinition(dto.sentimentScoreDefinition());
        entity.setRelevanceScoreDefinition(dto.relevanceScoreDefinition());
        return entity;
    }

    public List<NewsTickerEntity> toNewsTickerEntities(NewsArticleDto dto) {
        if (dto.tickerSentiment() == null) {
            return List.of();
        }
        return dto.tickerSentiment().stream().map(s -> {
            NewsTickerEntity entity = new NewsTickerEntity();
            entity.setNewsId(dto.id());
            entity.setTicker(s.ticker());
            entity.setTimePublished(DateTimeUtil.toOffsetDateTime(dto.timePublished()));
            entity.setRelevanceScore(s.relevanceScore());
            entity.setTickerSentimentScore(s.tickerSentimentScore());
            entity.setTickerSentimentLabel(s.tickerSentimentLabel());
            return entity;
        }).collect(Collectors.toList());
    }
}
