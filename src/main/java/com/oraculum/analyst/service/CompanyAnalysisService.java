package com.oraculum.analyst.service;

import com.oraculum.analyst.api.CompanyAnalysisApi;
import com.oraculum.analyst.domain.CompanyAnalysisEntity;

import java.util.UUID;

public interface CompanyAnalysisService extends CompanyAnalysisApi {
    CompanyAnalysisEntity createOrUpdateAnalysis(CompanyAnalysisEntity entity);

    boolean isAnalysisCompleted(UUID id);
}
