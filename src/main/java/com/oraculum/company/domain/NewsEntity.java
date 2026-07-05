package com.oraculum.company.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import com.oraculum.company.api.dto.NewsArticleDto.TopicRelevanceDto;

@Entity
@Table(name = "t_news", indexes = {@Index(name = "ix_news_time_published", columnList = "time_published")})
@IdClass(NewsEntity.NewsId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsEntity {

    @Id
    @Column(length = 64)
    private String id;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String title;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;
    @Id
    @Column(name = "time_published", nullable = false)
    private OffsetDateTime timePublished;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> authors;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;
    @Column(length = 255)
    private String source;
    @Column(name = "category_within_source", length = 255)
    private String categoryWithinSource;
    @Column(name = "source_domain", length = 255)
    private String sourceDomain;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<TopicRelevanceDto> topics;
    @Column(name = "overall_sentiment_score")
    private Float overallSentimentScore;
    @Column(name = "overall_sentiment_label", length = 50)
    private String overallSentimentLabel;
    @Column(name = "extracted_at", nullable = false)
    private OffsetDateTime extractedAt;
    @Column(name = "sentiment_score_definition", columnDefinition = "TEXT")
    private String sentimentScoreDefinition;
    @Column(name = "relevance_score_definition", columnDefinition = "TEXT")
    private String relevanceScoreDefinition;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class NewsId implements Serializable {
        private String id;
        private OffsetDateTime timePublished;
    }
}
