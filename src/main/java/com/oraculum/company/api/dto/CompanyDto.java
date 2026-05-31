package com.oraculum.company.api.dto;

import com.oraculum.company.domain.CompanyEntity;

import java.time.OffsetDateTime;

public record CompanyDto(
    Integer id,
    String ticker,
    String market,
    String companyName,
    String industryId,
    String industryName,
    String sectorName,
    String isin,
    String description,
    Long employeeCount,
    String currency,
    String cik,
    OffsetDateTime extractedAt
) {
    public static CompanyDto fromEntity(CompanyEntity entity) {
        if (entity == null) return null;
        return new CompanyDto(
            entity.getId(),
            entity.getTicker(),
            entity.getMarket(),
            entity.getCompanyName(),
            entity.getIndustryId(),
            entity.getIndustryName(),
            entity.getSectorName(),
            entity.getIsin(),
            entity.getDescription(),
            entity.getEmployeeCount(),
            entity.getCurrency(),
            entity.getCik(),
            entity.getExtractedAt()
        );
    }
}