package com.oraculum.company.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "t_insider_transaction_ticker")
@IdClass(InsiderTransactionTickerEntity.InsiderId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsiderTransactionTickerEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Id
    @Column(name = "filing_date")
    private LocalDateTime filingDate;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(name = "insider_name")
    private String insiderName;

    private String title;

    @Column(name = "trade_type", length = 50)
    private String tradeType;

    @Column(length = 3)
    private String currency;

    private BigDecimal price;

    private BigDecimal qty;

    private BigDecimal owned;

    @Column(name = "delta_own")
    private BigDecimal deltaOwn;

    private BigDecimal value;

    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class InsiderId implements Serializable {
        private String id;
        private LocalDateTime filingDate;
    }
}
