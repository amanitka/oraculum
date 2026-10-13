package com.oraculum.company.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReverseDcfSensitivityMatrixDto(
        @JsonProperty("discount_rates_pct") List<Double> discountRatesPct,
        @JsonProperty("terminal_rates_pct") List<Double> terminalRatesPct,
        @JsonProperty("cells") List<ReverseDcfMatrixCellDto> cells
) {}
