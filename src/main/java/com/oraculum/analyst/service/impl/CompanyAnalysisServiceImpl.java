package com.oraculum.analyst.service.impl;

import com.oraculum.analyst.domain.CompanyAnalysisEntity;
import com.oraculum.analyst.repository.CompanyAnalysisRepository;
import com.oraculum.analyst.service.CompanyAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyAnalysisServiceImpl implements CompanyAnalysisService {

    private final CompanyAnalysisRepository companyAnalysisRepository;

    @Override
    @Transactional
    public CompanyAnalysisEntity createOrUpdateAnalysis(CompanyAnalysisEntity entity) {
        return companyAnalysisRepository.save(entity);
    }
}
