package com.oraculum.company.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "t_share_price")
@IdClass(SharePriceId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SharePriceEntity {

    @Id
    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Id
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(nullable = false, length = 10)
    private String market;

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
}