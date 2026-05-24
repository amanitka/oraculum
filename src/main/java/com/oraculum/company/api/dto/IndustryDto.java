package com.oraculum.company.api.dto;

import com.oraculum.company.domain.IndustryEntity;

import java.time.OffsetDateTime;

public record IndustryDto(
    String industryId,
    String sectorName,
    String industryName,
    String statementTemplate,
    OffsetDateTime extractedAt
) {
    public static IndustryDto fromEntity(IndustryEntity entity) {
        if (entity == null) return null;
        return new IndustryDto(
            entity.getIndustryId(),
            entity.getSectorName(),
            entity.getIndustryName(),
            entity.getStatementTemplate(),
            entity.getExtractedAt()
        );
    }

    public IndustryEntity toEntity() {
        IndustryEntity entity = new IndustryEntity();
        entity.setIndustryId(this.industryId);
        entity.setSectorName(this.sectorName);
        entity.setIndustryName(this.industryName);
        entity.setStatementTemplate(this.statementTemplate);
        entity.setExtractedAt(this.extractedAt);
        return entity;
    }
}