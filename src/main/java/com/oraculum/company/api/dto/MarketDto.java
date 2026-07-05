package com.oraculum.company.api.dto;

import com.oraculum.company.domain.MarketEntity;

import java.time.OffsetDateTime;

public record MarketDto(
    String marketId,
    String marketName,
    String currency,
    OffsetDateTime extractedAt
) {
    public static MarketDto fromEntity(MarketEntity entity) {
        if (entity == null) return null;
        return new MarketDto(
            entity.getMarketId(),
            entity.getMarketName(),
            entity.getCurrency(),
            entity.getExtractedAt()
        );
    }

    public MarketEntity toEntity() {
        MarketEntity entity = new MarketEntity();
        entity.setMarketId(this.marketId);
        entity.setMarketName(this.marketName);
        entity.setCurrency(this.currency);
        entity.setExtractedAt(this.extractedAt);
        return entity;
    }
}
