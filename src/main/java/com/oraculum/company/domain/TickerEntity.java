package com.oraculum.company.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "t_ticker", uniqueConstraints = {@UniqueConstraint(name = "uq_ticker_ticker_market", columnNames = {
        "ticker", "market"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TickerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String ticker;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "industry_id")
    private String industryId;

    @Column(name = "industry_name")
    private String industryName;

    @Column(name = "sector_name")
    private String sectorName;

    @Column(name = "isin")
    private String isin;

    @Column(name = "description")
    private String description;

    @Column(name = "employee_count")
    private Long employeeCount;

    @Column(nullable = false)
    private String market;

    @Column(nullable = false)
    private String currency;

    @Column(name = "cik")
    private String cik;

    @Column(name = "extracted_at", nullable = false)
    private OffsetDateTime extractedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}