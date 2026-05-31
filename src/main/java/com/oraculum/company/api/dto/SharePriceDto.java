package com.oraculum.company.api.dto;

import com.oraculum.company.domain.SharePriceEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record SharePriceDto(
    Integer companyId,
    LocalDate tradeDate,
    String market,
    String currency,
    Float open,
    Float high,
    Float low,
    Float close,
    Float adjClose,
    Long volume,
    Long sharesOutstanding,
    Float dividend,
    OffsetDateTime extractedAt
) {
    public static SharePriceDto fromEntity(SharePriceEntity entity) {
        if (entity == null) return null;
        return new SharePriceDto(
            entity.getCompanyId(),
            entity.getTradeDate(),
            entity.getMarket(),
            entity.getCurrency(),
            entity.getOpen(),
            entity.getHigh(),
            entity.getLow(),
            entity.getClose(),
            entity.getAdjClose(),
            entity.getVolume(),
            entity.getSharesOutstanding(),
            entity.getDividend(),
            entity.getExtractedAt()
        );
    }
}