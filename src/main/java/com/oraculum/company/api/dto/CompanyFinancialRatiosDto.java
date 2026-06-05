package com.oraculum.company.api.dto;

import com.oraculum.company.api.domain.StatementTemplate;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.domain.CompanyFinancialRatiosEntity;

import java.time.LocalDate;

public record CompanyFinancialRatiosDto(String id,
                                        int companyId,
                                        String ticker,
                                        String currency,
                                        StatementTemplate template,
                                        StatementVariant variant,
                                        int fiscalYear,
                                        String fiscalPeriod,
                                        LocalDate reportDate,
                                        LocalDate publishDate,
                                        LocalDate restatedDate,
                                        Float ebitda,
                                        Float freeCashFlow,
                                        Float ncav,
                                        Float netNetWorkingCapital,
                                        Float sharesStabilized,
                                        Float returnOnEquity,
                                        Float netMargin,
                                        Float revenue,
                                        Float netIncome,
                                        Float grossMargin,
                                        Float operatingMargin,
                                        Float fcfMargin,
                                        Float quickRatio,
                                        Float interestCoverageRatio) {
    public static CompanyFinancialRatiosDto fromEntity(CompanyFinancialRatiosEntity entity) {
        if (entity == null)
            return null;
        return new CompanyFinancialRatiosDto(entity.getId(),
                entity.getCompanyId(),
                entity.getTicker(),
                entity.getCurrency(),
                entity.getTemplate(),
                entity.getVariant(),
                entity.getFiscalYear(),
                entity.getFiscalPeriod(),
                entity.getReportDate(),
                entity.getPublishDate(),
                entity.getRestatedDate(),
                entity.getEbitda(),
                entity.getFreeCashFlow(),
                entity.getNcav(),
                entity.getNetNetWorkingCapital(),
                entity.getSharesStabilized(),
                entity.getReturnOnEquity(),
                entity.getNetMargin(),
                entity.getRevenue(),
                entity.getNetIncome(),
                entity.getGrossMargin(),
                entity.getOperatingMargin(),
                entity.getFcfMargin(),
                entity.getQuickRatio(),
                entity.getInterestCoverageRatio());
    }
}