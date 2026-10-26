package com.oraculum.analyst.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.company.api.dto.SharePriceSignalDto;

import java.time.LocalDate;

public record SharePriceSignalSlim(
        @JsonProperty("trade_date") LocalDate tradeDate,
        @JsonProperty("share_price") Float sharePrice,
        @JsonProperty("ma_20") Float ma20,
        @JsonProperty("ma_50") Float ma50,
        @JsonProperty("ma_200") Float ma200,
        @JsonProperty("pct_from_20d_ma") Float pctFrom20dMa,
        @JsonProperty("pct_from_50d_ma") Float pctFrom50dMa,
        @JsonProperty("pct_from_200d_ma") Float pctFrom200dMa,
        @JsonProperty("high_52w") Float high52w,
        @JsonProperty("low_52w") Float low52w,
        @JsonProperty("drawdown_52w_pct") Float drawdown52wPct,
        @JsonProperty("range_position_52w_pct") Float rangePosition52wPct,
        @JsonProperty("down_days_high_vol_5d") Integer downDaysHighVol5d,
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
        @JsonProperty("momentum_trend_score") Integer financialTrendScore,
        @JsonProperty("market_capitalization") Float marketCapitalization,
        @JsonProperty("enterprise_value") Float enterpriseValue,
        @JsonProperty("enterprise_value_to_revenue") Float enterpriseValueToRevenue,
        @JsonProperty("enterprise_value_to_fcf") Float enterpriseValueToFreeCashFlow,
        @JsonProperty("citation_id") String citationId
) {
    public static SharePriceSignalSlim from(SharePriceSignalDto dto, String citationId) {
        if (dto == null) return null;
        return new SharePriceSignalSlim(
                dto.tradeDate(),
                dto.sharePrice(),
                dto.ma20(),
                dto.ma50(),
                dto.ma200(),
                dto.pctFrom20dMa(),
                dto.pctFrom50dMa(),
                dto.pctFrom200dMa(),
                dto.high52w(),
                dto.low52w(),
                dto.drawdown52wPct(),
                dto.rangePosition52wPct(),
                dto.downDaysHighVol5d(),
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
                dto.financialTrendScore(),
                dto.marketCapitalization(),
                dto.enterpriseValue(),
                dto.enterpriseValueToRevenue(),
                dto.enterpriseValueToFreeCashFlow(),
                citationId
        );
    }
}
