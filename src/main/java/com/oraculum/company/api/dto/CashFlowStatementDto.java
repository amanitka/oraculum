package com.oraculum.company.api.dto;

import com.oraculum.company.domain.CashFlowStatementEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CashFlowStatementDto(
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
    public static CashFlowStatementDto fromEntity(CashFlowStatementEntity entity) {
        if (entity == null) return null;
        return new CashFlowStatementDto(
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