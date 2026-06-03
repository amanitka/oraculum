package com.oraculum.analyst.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "t_company_analysis", indexes = {@Index(name = "ix_company_analysis_company_id", columnList =
        "company_id"), @Index(name = "ix_company_analysis_ticker_market_created", columnList = "ticker, market, " +
        "created_at"), @Index(name = "ix_company_analysis_status_created", columnList = "status, created_at")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyAnalysisEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Column(nullable = false)
    private String market;

    @Column(nullable = false)
    private String ticker;

    @Column(name = "analysis_date", nullable = false)
    private LocalDate analysisDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AnalysisStatus status;

    @Lob
    @Column(name = "report")
    private String report;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private AnalysisOutlook outlook;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private AnalysisRecommendation recommendation;

    private Integer conviction;

    @Column(name = "analysis_data", columnDefinition = "jsonb")
    private String analysisData;

    @Lob
    private String error;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}