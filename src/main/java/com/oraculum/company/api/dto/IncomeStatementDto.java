package com.oraculum.company.api.dto;

import com.oraculum.company.api.domain.StatementTemplate;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.domain.IncomeStatementEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record IncomeStatementDto(String id,
                                 Integer companyId,
                                 String market,
                                 StatementTemplate template,
                                 StatementVariant variant,
                                 String currency,
                                 int fiscalYear,
                                 String fiscalPeriod,
                                 LocalDate reportDate,
                                 LocalDate publishDate,
                                 LocalDate restatedDate,
                                 OffsetDateTime extractedAt) {
    public static IncomeStatementDto fromEntity(IncomeStatementEntity entity) {
        if (entity == null)
            return null;
        return new IncomeStatementDto(entity.getId(),
                entity.getCompanyId(),
                entity.getMarket(),
                entity.getTemplate(),
                entity.getVariant(),
                entity.getCurrency(),
                entity.getFiscalYear(),
                entity.getFiscalPeriod(),
                entity.getReportDate(),
                entity.getPublishDate(),
                entity.getRestatedDate(),
                entity.getExtractedAt());
    }
}