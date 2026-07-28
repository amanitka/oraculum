package com.oraculum.analyst.api;

import com.oraculum.analyst.api.dto.CompanyAnalysisViewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyAnalysisApi {
    Page<CompanyAnalysisViewDto> getLatestCompanyAnalyses(Pageable pageable);

    List<CompanyAnalysisViewDto> getHistoricalAnalysesByCompanyId(Integer companyId);

    Optional<CompanyAnalysisViewDto> getAnalysisById(UUID id);
}
