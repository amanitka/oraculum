package com.oraculum.company.api.dto;

import com.oraculum.company.domain.TickerEntity;

import java.time.OffsetDateTime;

public record TickerDto(
    String ticker,
    String providerId,
    String providerName,
    String companyName,
    String industryId,
    String industryName,
    String sectorName,
    String isin,
    String description,
    Long employeeCount,
    String market,
    String currency,
    String cik,
    OffsetDateTime extractedAt
) {
    public static TickerDto fromEntity(TickerEntity entity) {
        if (entity == null) return null;
        return new TickerDto(
            entity.getTicker(),
            entity.getProviderId(),
            entity.getProviderName(),
            entity.getCompanyName(),
            entity.getIndustryId(),
            entity.getIndustryName(),
            entity.getSectorName(),
            entity.getIsin(),
            entity.getDescription(),
            entity.getEmployeeCount(),
            entity.getMarket(),
            entity.getCurrency(),
            entity.getCik(),
            entity.getExtractedAt()
        );
    }
}