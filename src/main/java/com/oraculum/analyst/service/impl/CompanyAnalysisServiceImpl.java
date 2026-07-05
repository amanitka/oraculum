package com.oraculum.analyst.service.impl;

import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import com.oraculum.analyst.domain.CompanyAnalysisEntity;
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
    public Page<CompanyAnalysisDto> getCompanyAnalysisList(Pageable pageable) {
        return companyAnalysisRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(CompanyAnalysisDto::fromEntity);
    }

}
