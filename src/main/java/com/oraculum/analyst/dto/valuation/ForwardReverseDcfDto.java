package com.oraculum.analyst.dto.valuation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.company.api.dto.ValuationZone;
import lombok.Builder;

import java.util.List;

/**
 * Result of the Two-Stage Reverse DCF calculation with derived Forward FCF proxy,
 * FCF margin sensitivity band, and unified valuation zones.
 */
@Builder
public record ForwardReverseDcfDto(
        @JsonProperty("market_cap_usd") Double marketCapUsd,
        @JsonProperty("fy1_fcf_proxy_usd") Double fy1FcfProxyUsd,
        @JsonProperty("fy2_fcf_proxy_usd") Double fy2FcfProxyUsd,
        @JsonProperty("fcf_proxy_derivation_method") String fcfProxyDerivationMethod,
        @JsonProperty("flag_fcf_proxy_divergence") Boolean flagFcfProxyDivergence,
        @JsonProperty("fcf_proxy_divergence_pct") Float fcfProxyDivergencePct,
        @JsonProperty("implied_growth_years_3_to_10_pct") Float impliedGrowthYears3To10Pct,
        @JsonProperty("relative_valuation_zone") ValuationZone relativeValuationZone,
        @JsonProperty("margin_sensitivity") List<FcfMarginSensitivityEntryDto> marginSensitivity,
        @JsonProperty("consensus_growth_rate_pct") Float consensusGrowthRatePct,
        @JsonProperty("historical_cagr_pct") Float historicalCagrPct,
        @JsonProperty("interpretation") String interpretation,
        @JsonProperty("is_clipped") boolean isClipped
) {
    public ForwardReverseDcfDto {
        marginSensitivity = marginSensitivity != null ? marginSensitivity : List.of();
    }
}
