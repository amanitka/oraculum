package com.oraculum.analyst.api.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AnalysisStatus {
    PENDING("Pending"),
    RUNNING("Running"),
    COMPLETED("Completed"),
    FAILED("Failed");

    private final String displayName;

    public boolean isCompleted() {
        return COMPLETED.equals(this);
    }
}