package com.oraculum.analyst.dto.valuation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record OperatingEarningsSummaryDto(
        @JsonProperty("operating_eps") Float operatingEps,
        @JsonProperty("operating_pe") Float operatingPe,
        @JsonProperty("gaap_pe") Float gaapPe,
        @JsonProperty("effective_tax_rate") Float effectiveTaxRate,
        @JsonProperty("non_operating_income_pretax_pct") Float nonOperatingIncomePretaxPct,
        @JsonProperty("flag_high_non_operating_income") Boolean flagHighNonOperatingIncome,
        @JsonProperty("note") String note
) {
}
