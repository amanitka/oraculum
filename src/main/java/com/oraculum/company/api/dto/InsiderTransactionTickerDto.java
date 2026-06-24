package com.oraculum.company.api.dto;

import com.oraculum.company.domain.InsiderTransactionTickerEntity;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record InsiderTransactionTickerDto(
    String id,
    String ticker,
    String insiderName,
    String title,
    String tradeType,
    String currency,
    BigDecimal price,
    BigDecimal qty,
    BigDecimal owned,
    BigDecimal deltaOwn,
    BigDecimal value,
    LocalDateTime filingDate,
    LocalDate tradeDate
) {
    public static InsiderTransactionTickerDto fromEntity(InsiderTransactionTickerEntity entity) {
        return InsiderTransactionTickerDto.builder()
                .id(entity.getId())
                .ticker(entity.getTicker())
                .insiderName(entity.getInsiderName())
                .title(entity.getTitle())
                .tradeType(entity.getTradeType())
                .currency(entity.getCurrency())
                .price(entity.getPrice())
                .qty(entity.getQty())
                .owned(entity.getOwned())
                .deltaOwn(entity.getDeltaOwn())
                .value(entity.getValue())
                .filingDate(entity.getFilingDate())
                .tradeDate(entity.getTradeDate())
                .build();
    }
}
