package com.oraculum.analyst.api.dto;

import com.oraculum.analyst.domain.CompanyAnalysisViewEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CompanyAnalysisViewDto extends CompanyAnalysisDto {

    private String companyName;
    private String industryName;
    private String sectorName;
    private Boolean isLatest;
    private Long totalAnalyses;

    public static CompanyAnalysisViewDto fromEntity(CompanyAnalysisViewEntity entity) {
        if (entity == null) {
            return null;
        }
        CompanyAnalysisViewDto dto = new CompanyAnalysisViewDto();
        dto.setId(entity.getId());
        dto.setCompanyId(entity.getCompanyId());
        dto.setMarket(entity.getMarket());
        dto.setTicker(entity.getTicker());
        dto.setAnalysisDate(entity.getAnalysisDate());
        dto.setStatus(entity.getStatus());
        dto.setReport(entity.getReport());
        dto.setSummary(entity.getSummary());
        dto.setOutlook(entity.getOutlook());
        dto.setRecommendation(entity.getRecommendation());
        dto.setConviction(entity.getConviction());
        dto.setAnalysisData(entity.getAnalysisData());
        dto.setError(entity.getError());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCompanyName(entity.getCompanyName());
        dto.setIndustryName(entity.getIndustryName());
        dto.setSectorName(entity.getSectorName());
        dto.setIsLatest(entity.getIsLatest());
        dto.setTotalAnalyses(entity.getTotalAnalyses());
        return dto;
    }
}
