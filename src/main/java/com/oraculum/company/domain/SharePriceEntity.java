package com.oraculum.company.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "t_share_price", uniqueConstraints = {@UniqueConstraint(name = "uq_share_price_composite", columnNames
        = {"ticker", "market", "trade_date"})})
@IdClass(SharePriceEntity.SharePriceId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SharePriceEntity {

    @Id
    @Column(nullable = false)
    private String ticker;
    @Id
    @Column(nullable = false)
    private String market;
    @Id
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "sim_fin_id")
    private Integer simFinId;
    @Column(name = "currency")
    private String currency;
    @Column(name = "open")
    private Float open;
    @Column(name = "high")
    private Float high;
    @Column(name = "low")
    private Float low;
    @Column(name = "close")
    private Float close;
    @Column(name = "adj_close")
    private Float adjClose;
    @Column(name = "volume")
    private Long volume;
    @Column(name = "shares_outstanding")
    private Long sharesOutstanding;
    @Column(name = "dividend")
    private Float dividend;
    @Column(name = "extracted_at", nullable = false)
    private OffsetDateTime extractedAt;
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
    public static class SharePriceId implements Serializable {
        private String ticker;
        private String market;
        private LocalDate tradeDate;
    }
}