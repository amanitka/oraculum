package com.oraculum.analyst.domain;

import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.AnalysisRecommendation;
import com.oraculum.analyst.api.domain.AnalysisStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "v_company_analysis")
@Immutable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyAnalysisViewEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "industry_name")
    private String industryName;

    @Column(name = "sector_name")
    private String sectorName;

    private String market;

    private String ticker;

    @Column(name = "analysis_date")
    private LocalDate analysisDate;

    @Enumerated(EnumType.STRING)
    private AnalysisStatus status;

    private String report;

    @Column(name = "summary", columnDefinition = "json")
    private String summary;

    @Enumerated(EnumType.STRING)
    private AnalysisOutlook outlook;

    @Enumerated(EnumType.STRING)
    private AnalysisRecommendation recommendation;

    private Integer conviction;

    @Column(name = "analysis_data", columnDefinition = "json")
    private String analysisData;

    private String error;

    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "is_latest")
    private Boolean isLatest;

    @Column(name = "total_analyses")
    private Long totalAnalyses;
}
