package com.oraculum.company.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "v_ticker_news_sentiment")
@Getter
public class TickerNewsSentimentEntity {

    @Id
    @Column(name = "ticker")
    private String ticker;

    @Column(name = "news_count_7d")
    private Integer newsCount7d;

    @Column(name = "news_sentiment_7d")
    private Float newsSentiment7d;

    @Column(name = "avg_relevance_7d")
    private Float avgRelevance7d;

    @Column(name = "news_sentiment_label_7d")
    private String newsSentimentLabel7d;

    @Column(name = "news_count_14d")
    private Integer newsCount14d;

    @Column(name = "news_sentiment_14d")
    private Float newsSentiment14d;

    @Column(name = "avg_relevance_14d")
    private Float avgRelevance14d;

    @Column(name = "news_sentiment_label_14d")
    private String newsSentimentLabel14d;

    @Column(name = "news_count_30d")
    private Integer newsCount30d;

    @Column(name = "news_sentiment_30d")
    private Float newsSentiment30d;

    @Column(name = "avg_relevance_30d")
    private Float avgRelevance30d;

    @Column(name = "news_sentiment_label_30d")
    private String newsSentimentLabel30d;
}
