package com.oraculum.company.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_screener_master")
@Getter
@Setter
public class ScreenerMasterEntity extends BaseScreenerEntity {

    @Column(name = "quality_rank")
    private Long qualityRank;

    @Column(name = "value_rank")
    private Long valueRank;

    @Column(name = "fscore_rank")
    private Long fscoreRank;

    @Column(name = "news_sentiment_score")
    private Float newsSentimentScore;

    @Column(name = "news_sentiment_label")
    private String newsSentimentLabel;

    @Column(name = "news_count_30d")
    private Integer newsCount30d;
}
