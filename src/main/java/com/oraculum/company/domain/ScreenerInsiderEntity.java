package com.oraculum.company.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_screener_insider_activity")
@Getter
@Setter
public class ScreenerInsiderEntity extends BaseScreenerEntity {

    // 3 Month
    @Column(name = "buys_value_3m")
    private Double buysValue3m;

    @Column(name = "sells_value_3m")
    private Double sellsValue3m;

    @Column(name = "csuite_buys_count_3m")
    private Integer csuiteBuysCount3m;

    @Column(name = "csuite_buys_value_3m")
    private Double csuiteBuysValue3m;

    // 6 Month
    @Column(name = "buys_value_6m")
    private Double buysValue6m;

    @Column(name = "sells_value_6m")
    private Double sellsValue6m;

    @Column(name = "csuite_buys_count_6m")
    private Integer csuiteBuysCount6m;

    @Column(name = "csuite_buys_value_6m")
    private Double csuiteBuysValue6m;

    // 12 Month (LTM)
    @Column(name = "buys_value_12m")
    private Double buysValue12m;

    @Column(name = "sells_value_12m")
    private Double sellsValue12m;

    @Column(name = "csuite_buys_count_12m")
    private Integer csuiteBuysCount12m;

    @Column(name = "csuite_buys_value_12m")
    private Double csuiteBuysValue12m;

    @Column(name = "has_cluster_buy")
    private Boolean hasClusterBuy;

    // News sentiment
    @Column(name = "news_sentiment_score")
    private Float newsSentimentScore;

    @Column(name = "news_sentiment_label")
    private String newsSentimentLabel;

    @Column(name = "news_count_30d")
    private Integer newsCount30d;

}
