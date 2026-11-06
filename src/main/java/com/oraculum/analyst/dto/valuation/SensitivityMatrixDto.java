package com.oraculum.analyst.dto.valuation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record SensitivityMatrixDto(
        @JsonProperty("target_fiscal_year") String targetFiscalYear,
        @JsonProperty("eps_stagnant") Float epsStagnant,
        @JsonProperty("eps_low") Float epsLow,
        @JsonProperty("eps_avg") Float epsAvg,
        @JsonProperty("eps_high") Float epsHigh,
        @JsonProperty("reference_multiples") List<Float> referenceMultiples,
        @JsonProperty("multiple_anchors") List<MultipleAnchorDto> multipleAnchors,
        @JsonProperty("implied_eps_by_multiple") List<ImpliedEpsEntryDto> impliedEpsByMultiple,
        @JsonProperty("matrix_summary") MatrixSummaryDto matrixSummary,
        @JsonProperty("cells") List<SensitivityCellDto> cells,
        @JsonProperty("disclaimer") String disclaimer
) {
    public SensitivityMatrixDto {
        referenceMultiples = referenceMultiples != null ? referenceMultiples : List.of();
        multipleAnchors = multipleAnchors != null ? multipleAnchors : List.of();
        impliedEpsByMultiple = impliedEpsByMultiple != null ? impliedEpsByMultiple : List.of();
        cells = cells != null ? cells : List.of();
    }
}
