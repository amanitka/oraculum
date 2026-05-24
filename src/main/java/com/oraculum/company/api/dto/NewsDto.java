package com.oraculum.company.api.dto;

import com.oraculum.company.domain.NewsEntity;

import java.time.OffsetDateTime;

public record NewsDto(String id, String title, String url, OffsetDateTime timePublished, String authors, String summary,
                      String source, String categoryWithinSource, String sourceDomain, String topics,
                      Float overallSentimentScore, String overallSentimentLabel, OffsetDateTime extractedAt,
                      String sentimentScoreDefinition, String relevanceScoreDefinition) {
    public static NewsDto fromEntity(NewsEntity entity) {
        if (entity == null) return null;
        return new NewsDto(entity.getId(), entity.getTitle(), entity.getUrl(), entity.getTimePublished(),
                entity.getAuthors(), entity.getSummary(), entity.getSource(), entity.getCategoryWithinSource(),
                entity.getSourceDomain(), entity.getTopics(), entity.getOverallSentimentScore(),
                entity.getOverallSentimentLabel(), entity.getExtractedAt(), entity.getSentimentScoreDefinition(),
                entity.getRelevanceScoreDefinition());
    }

    public NewsEntity toEntity() {
        NewsEntity entity = new NewsEntity();
        entity.setId(this.id);
        entity.setTitle(this.title);
        entity.setUrl(this.url);
        entity.setTimePublished(this.timePublished);
        entity.setAuthors(this.authors);
        entity.setSummary(this.summary);
        entity.setSource(this.source);
        entity.setCategoryWithinSource(this.categoryWithinSource);
        entity.setSourceDomain(this.sourceDomain);
        entity.setTopics(this.topics);
        entity.setOverallSentimentScore(this.overallSentimentScore);
        entity.setOverallSentimentLabel(this.overallSentimentLabel);
        entity.setExtractedAt(this.extractedAt);
        entity.setSentimentScoreDefinition(this.sentimentScoreDefinition);
        entity.setRelevanceScoreDefinition(this.relevanceScoreDefinition);
        return entity;
    }
}