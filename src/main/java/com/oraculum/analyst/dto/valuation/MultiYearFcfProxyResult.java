package com.oraculum.analyst.dto.valuation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record MultiYearFcfProxyResult(
        @JsonProperty("fy1_fcf_proxy") Double fy1FcfProxy,
        @JsonProperty("fy2_fcf_proxy") Double fy2FcfProxy,
        @JsonProperty("derivation_method") String derivationMethod,
        @JsonProperty("flag_divergence") Boolean flagDivergence,
        @JsonProperty("max_divergence_pct") Float maxDivergencePct
) {
}
