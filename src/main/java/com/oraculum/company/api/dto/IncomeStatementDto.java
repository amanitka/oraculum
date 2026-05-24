package com.oraculum.company.api.dto;

import com.oraculum.company.domain.IncomeStatementEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record IncomeStatementDto(
    String compositeKey,
    String ticker,
    int simfinId,
    String template,
    String variant,
    String currency,
    int fiscalYear,
    String fiscalPeriod,
    LocalDate reportDate,
    LocalDate publishDate,
    LocalDate restatedDate,
    OffsetDateTime extractedAt
) {
    public static IncomeStatementDto fromEntity(IncomeStatementEntity entity) {
        if (entity == null) return null;
        return new IncomeStatementDto(
            entity.getCompositeKey(),
            entity.getTicker(),
            entity.getSimfinId(),
            entity.getTemplate(),
            entity.getVariant(),
            entity.getCurrency(),
            entity.getFiscalYear(),
            entity.getFiscalPeriod(),
            entity.getReportDate(),
            entity.getPublishDate(),
            entity.getRestatedDate(),
            entity.getExtractedAt()
        );
    }
}