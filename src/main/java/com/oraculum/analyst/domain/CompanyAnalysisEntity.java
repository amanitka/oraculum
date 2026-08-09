package com.oraculum.analyst.domain;

import com.oraculum.analyst.api.domain.AnalysisStatus;
import com.oraculum.analyst.api.dto.AnalysisResult;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "t_company_analysis", indexes = {
        @Index(name = "ix_company_analysis_company_id", columnList = "company_id"),
        @Index(name = "ix_company_analysis_ticker_market_created", columnList = "ticker, market, created_at"),
        @Index(name = "ix_company_analysis_status_created", columnList = "status, created_at")
})
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

    /** Automatically converted to/from JSONB by Hibernate. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_result", columnDefinition = "jsonb")
    private AnalysisResult analysisResult;

    /** Full agent trace for debugging — stored separately from the business result. */
    @Column(name = "analysis_data", columnDefinition = "json")
    @ColumnTransformer(write = "?::json")
    private String analysisData;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(name = "requested_by")
    private Long requestedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
