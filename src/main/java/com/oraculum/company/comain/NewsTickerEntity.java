package com.oraculum.company.comain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Entity
@Table(name = "t_news_ticker", indexes = {@Index(name = "ix_news_ticker_ticker", columnList = "ticker")})
@IdClass(NewsTickerEntity.NewsTickerId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsTickerEntity {

    @Id
    @Column(name = "news_id", length = 64)
    private String newsId;
    @Id
    @Column(length = 16)
    private String ticker;
    @Id
    @Column(name = "time_published")
    private OffsetDateTime timePublished;
    @Column(name = "relevance_score")
    private Float relevanceScore;
    @Column(name = "ticker_sentiment_score")
    private Float tickerSentimentScore;
    @Column(name = "ticker_sentiment_label", length = 50)
    private String tickerSentimentLabel;
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
    public static class NewsTickerId implements Serializable {
        private String newsId;
        private String ticker;
        private OffsetDateTime timePublished;
    }
}