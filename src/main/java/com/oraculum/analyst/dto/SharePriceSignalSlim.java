package com.oraculum.analyst.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.company.api.dto.SharePriceSignalDto;

import java.time.LocalDate;

public record SharePriceSignalSlim(
        @JsonProperty("trade_date") LocalDate tradeDate,
        @JsonProperty("share_price") Float sharePrice,
        @JsonProperty("pct_from_50d_ma") Float pctFrom50dMa,
        @JsonProperty("pct_from_200d_ma") Float pctFrom200dMa,
        @JsonProperty("volume_velocity") Float volumeVelocity,
        @JsonProperty("pe_ratio") Float peRatio,
        @JsonProperty("price_to_book") Float priceToBook,
        @JsonProperty("price_to_sales") Float priceToSales,
        @JsonProperty("price_to_fcf") Float priceToFcf,
        @JsonProperty("earnings_yield") Float earningsYield,
        @JsonProperty("fcf_yield") Float fcfYield,
        @JsonProperty("enterprise_value_to_ebitda") Float enterpriseValueToEbitda,
        @JsonProperty("return_on_equity") Float returnOnEquity,
        @JsonProperty("revenue_yoy_growth") Float revenueYoyGrowth,
        @JsonProperty("financial_trend_score") Integer financialTrendScore
) {
    public static SharePriceSignalSlim from(SharePriceSignalDto dto) {
        if (dto == null) return null;
        return new SharePriceSignalSlim(
                dto.tradeDate(),
                dto.sharePrice(),
                dto.pctFrom50dMa(),
                dto.pctFrom200dMa(),
                dto.volumeVelocity(),
                dto.peRatio(),
                dto.priceToBook(),
                dto.priceToSales(),
                dto.priceToFcf(),
                dto.earningsYield(),
                dto.fcfYield(),
                dto.enterpriseValueToEbitda(),
                dto.returnOnEquity(),
                dto.revenueYoyGrowth(),
                dto.financialTrendScore()
        );
    }
}
