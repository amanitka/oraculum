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
@Table(name = "t_analysis", indexes = {@Index(name = "ix_analysis_correlation_id", columnList = "correlation_id",
        unique = true), @Index(name = "ix_analysis_ticker_market_created", columnList = "ticker, market, created_at")
        , @Index(name = "ix_analysis_status_created", columnList = "status, created_at")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private String market;

    @Column(name = "analysis_date", nullable = false)
    private LocalDate analysisDate;

    @Column(nullable = false)
    private String status;

    @Lob
    @Column(name = "report_md")
    private String reportMd;

    private String verdict;

    private Integer conviction;

    @Column(columnDefinition = "jsonb")
    private String payload;

    @Lob
    private String error;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}