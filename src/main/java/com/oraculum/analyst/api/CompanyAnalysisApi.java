package com.oraculum.analyst.api;

import com.oraculum.analyst.api.dto.CompanyAnalysisViewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompanyAnalysisApi {
    Page<CompanyAnalysisViewDto> getLatestCompanyAnalyses(Pageable pageable);

    java.util.List<CompanyAnalysisViewDto> getHistoricalAnalysesByCompanyId(Integer companyId);
}
