package com.oraculum.analyst.dto.valuation;

public record ForwardDcfSolverResult(
        double impliedGrowth,
        boolean isClipped
) {
}
