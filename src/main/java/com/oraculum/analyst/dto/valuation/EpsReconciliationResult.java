package com.oraculum.analyst.dto.valuation;

public record EpsReconciliationResult(
        boolean divergence,
        String status,
        String note
) {
}
