package com.oraculum.analyst.api.dto;

import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.AnalysisRecommendation;
import com.oraculum.analyst.api.domain.AnalysisStatus;
import com.oraculum.analyst.domain.CompanyAnalysisEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyAnalysisDto {

    private UUID id;
    private Integer companyId;
    private String market;
    private String ticker;
    private LocalDate analysisDate;
    private AnalysisStatus status;
    private String report;
    private AnalysisOutlook outlook;
    private AnalysisRecommendation recommendation;
    private Integer conviction;
    private String analysisData;
    private String error;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static CompanyAnalysisDto fromEntity(CompanyAnalysisEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CompanyAnalysisDto(entity.getId(),
                entity.getCompanyId(),
                entity.getMarket(),
                entity.getTicker(),
                entity.getAnalysisDate(),
                entity.getStatus(),
                entity.getReport(),
                entity.getOutlook(),
                entity.getRecommendation(),
                entity.getConviction(),
                entity.getAnalysisData(),
                entity.getError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
