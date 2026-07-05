package com.oraculum.company.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "t_market")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketEntity {

    @Id
    @Column(name = "market_id")
    private String marketId;

    @Column(name = "market_name", nullable = false)
    private String marketName;

    @Column(name = "currency")
    private String currency;

    @Column(name = "extracted_at", nullable = false)
    private OffsetDateTime extractedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
