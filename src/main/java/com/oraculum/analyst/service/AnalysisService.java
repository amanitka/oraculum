package com.oraculum.analyst.service;

import com.oraculum.analyst.domain.CompanyAnalysis;

public interface AnalysisService {
    CompanyAnalysis runAnalysis(String ticker, String market);
}