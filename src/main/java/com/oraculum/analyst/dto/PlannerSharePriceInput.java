package com.oraculum.analyst.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.company.api.dto.SharePriceSignalDto;

import java.time.LocalDate;

public record PlannerSharePriceInput(
        @JsonProperty("trade_date") LocalDate tradeDate,
        @JsonProperty("share_price") Float sharePrice,
        @JsonProperty("pct_from_50d_ma") Float pctFrom50dMa,
        @JsonProperty("pct_from_200d_ma") Float pctFrom200dMa,
        @JsonProperty("volume_velocity") Float volumeVelocity,
        @JsonProperty("pe_ratio") Float peRatio,
        @JsonProperty("quality_score") Float qualityScore,
        @JsonProperty("piotroski_f_score") Integer piotroskiFScore,
        @JsonProperty("revenue_yoy_growth") Float revenueYoyGrowth
) {
    public static PlannerSharePriceInput from(SharePriceSignalDto dto) {
        if (dto == null) return null;
        return new PlannerSharePriceInput(
                dto.tradeDate(),
                dto.sharePrice(),
                dto.pctFrom50dMa(),
                dto.pctFrom200dMa(),
                dto.volumeVelocity(),
                dto.peRatio(),
                dto.qualityScore(),
                dto.piotroskiFScore(),
                dto.revenueYoyGrowth()
        );
    }
}
