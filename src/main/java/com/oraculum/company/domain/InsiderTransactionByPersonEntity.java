package com.oraculum.company.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_insider_transaction_by_person")
@Getter
@Setter
public class InsiderTransactionByPersonEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "ticker")
    private String ticker;

    @Column(name = "insider_name")
    private String insiderName;

    @Column(name = "title")
    private String title;

    // 1 Month
    @Column(name = "tx_count_1m")
    private Long txCount1m;

    @Column(name = "shares_bought_1m")
    private Double sharesBought1m;

    @Column(name = "shares_sold_1m")
    private Double sharesSold1m;

    @Column(name = "value_sold_1m")
    private Double valueSold1m;

    // 3 Month
    @Column(name = "tx_count_3m")
    private Long txCount3m;

    @Column(name = "shares_bought_3m")
    private Double sharesBought3m;

    @Column(name = "shares_sold_3m")
    private Double sharesSold3m;

    @Column(name = "value_sold_3m")
    private Double valueSold3m;

    // 6 Month
    @Column(name = "tx_count_6m")
    private Long txCount6m;

    @Column(name = "shares_bought_6m")
    private Double sharesBought6m;

    @Column(name = "shares_sold_6m")
    private Double sharesSold6m;

    @Column(name = "value_sold_6m")
    private Double valueSold6m;

    // 12 Month
    @Column(name = "tx_count_12m")
    private Long txCount12m;

    @Column(name = "shares_bought_12m")
    private Double sharesBought12m;

    @Column(name = "shares_sold_12m")
    private Double sharesSold12m;

    @Column(name = "value_sold_12m")
    private Double valueSold12m;

    // Holdings & stake change
    @Column(name = "initial_owned_12m")
    private Double initialOwned12m;

    @Column(name = "current_owned")
    private Double currentOwned;

    @Column(name = "stake_change_pct_12m")
    private Double stakeChangePct12m;
}
