package com.oraculum.company.api.dto;

import com.oraculum.company.api.domain.StatementTemplate;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.domain.BalanceSheetEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record BalanceSheetDto(String id,
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
                              OffsetDateTime extractedAt,
                              String statementData) {
    public static BalanceSheetDto fromEntity(BalanceSheetEntity entity) {
        if (entity == null)
            return null;
        return new BalanceSheetDto(entity.getId(),
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
                entity.getExtractedAt(),
                entity.getStatementData());
    }
}