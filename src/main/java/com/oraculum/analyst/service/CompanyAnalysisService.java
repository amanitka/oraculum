package com.oraculum.analyst.service;

import com.oraculum.analyst.api.CompanyAnalysisApi;
import com.oraculum.analyst.domain.CompanyAnalysisEntity;

public interface CompanyAnalysisService extends CompanyAnalysisApi {
    CompanyAnalysisEntity createOrUpdateAnalysis(CompanyAnalysisEntity entity);
}
