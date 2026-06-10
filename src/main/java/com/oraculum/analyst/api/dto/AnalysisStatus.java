package com.oraculum.analyst.api.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AnalysisStatus {
    PENDING("Pending"),
    RUNNING("Running"),
    COMPLETED("Completed"),
    FAILED("Failed");

    private final String name;

    public boolean isCompleted() {
        return COMPLETED.equals(this);
    }
}