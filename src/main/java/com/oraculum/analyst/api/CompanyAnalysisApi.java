package com.oraculum.analyst.api;

import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CompanyAnalysisApi {
    Page<CompanyAnalysisDto> getCompanyAnalysisList(Pageable pageable);
}
