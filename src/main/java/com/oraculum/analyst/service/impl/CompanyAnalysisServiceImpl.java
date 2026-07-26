package com.oraculum.analyst.service.impl;

import com.oraculum.analyst.api.dto.CompanyAnalysisViewDto;
import com.oraculum.analyst.domain.CompanyAnalysisEntity;
import com.oraculum.analyst.domain.CompanyAnalysisViewRepository;
import com.oraculum.analyst.repository.CompanyAnalysisRepository;
import com.oraculum.analyst.service.CompanyAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyAnalysisServiceImpl implements CompanyAnalysisService {

    private final CompanyAnalysisRepository companyAnalysisRepository;
    private final CompanyAnalysisViewRepository companyAnalysisViewRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompanyAnalysisEntity createOrUpdateAnalysis(CompanyAnalysisEntity entity) {
        return companyAnalysisRepository.save(entity);
    }

    @Override
    public boolean isAnalysisCompleted(UUID id) {
        var analysis = companyAnalysisRepository.findById(id);
        return analysis.isPresent() && analysis.get().getStatus().isCompleted();
    }

    @Override
    public Page<CompanyAnalysisViewDto> getLatestCompanyAnalyses(Pageable pageable) {
        return companyAnalysisViewRepository.findByIsLatestTrueOrderByCreatedAtDesc(pageable)
                .map(CompanyAnalysisViewDto::fromEntity);
    }

    @Override
    public java.util.List<CompanyAnalysisViewDto> getHistoricalAnalysesByCompanyId(Integer companyId) {
        return companyAnalysisViewRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(CompanyAnalysisViewDto::fromEntity)
                .toList();
    }
}
