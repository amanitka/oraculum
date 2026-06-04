package com.oraculum.analyst.api.dto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum AnalysisStatus {
    PENDING("Pending"),
    RUNNING("Running"),
    COMPLETED("Completed"),
    FAILED("Failed");

    private final String name;
}