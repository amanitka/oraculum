package com.oraculum.company.service.impl;

import com.oraculum.company.api.CompanyInstitutionalFlowApi;
import com.oraculum.company.api.dto.CompanyInstitutionalFlowDto;
import com.oraculum.company.repository.CompanyInstitutionalFlowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyInstitutionalFlowServiceImpl implements CompanyInstitutionalFlowApi {

    private final CompanyInstitutionalFlowRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, CompanyInstitutionalFlowDto> getLatestInstitutionalFlowByCompanyId() {
        return repository.findByQuarterIndex(1L)
                .stream()
                .map(CompanyInstitutionalFlowDto::fromEntity)
                .collect(Collectors.toMap(CompanyInstitutionalFlowDto::companyId, dto -> dto, (a, _) -> a));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, CompanyInstitutionalFlowDto> getInstitutionalFlowByPeriod(LocalDate reportPeriod) {
        if (reportPeriod == null) {
            return getLatestInstitutionalFlowByCompanyId();
        }
        return repository.findByReportPeriod(reportPeriod)
                .stream()
                .map(CompanyInstitutionalFlowDto::fromEntity)
                .collect(Collectors.toMap(CompanyInstitutionalFlowDto::companyId, dto -> dto, (a, _) -> a));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDate> getAvailableReportPeriods() {
        return repository.findDistinctReportPeriods();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyInstitutionalFlowDto> getInstitutionalFlowHistory(int companyId) {
        return repository.findByCompanyIdOrderByReportPeriodDesc(companyId)
                .stream()
                .map(CompanyInstitutionalFlowDto::fromEntity)
                .toList();
    }
}
