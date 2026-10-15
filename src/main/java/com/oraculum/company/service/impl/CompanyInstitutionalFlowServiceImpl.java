package com.oraculum.company.service.impl;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.company.api.CompanyInstitutionalFlowApi;
import com.oraculum.company.api.dto.CompanyInstitutionalFlowDto;
import com.oraculum.company.api.dto.SecFilerDto;
import com.oraculum.company.api.dto.SectorInstitutionalFlowTrendDto;
import com.oraculum.company.domain.SecFilerEntity;
import com.oraculum.company.repository.CompanyInstitutionalFlowRepository;
import com.oraculum.company.repository.SecFilerRepository;
import com.oraculum.company.repository.SectorInstitutionalFlowRepository;
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
    private final SectorInstitutionalFlowRepository sectorRepository;
    private final SecFilerRepository secFilerRepository;
    private final OraculumProperties properties;

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

    @Override
    @Transactional(readOnly = true)
    public List<SectorInstitutionalFlowTrendDto> getSectorInstitutionalFlowByPeriod(LocalDate reportPeriod) {
        if (reportPeriod == null) {
            List<LocalDate> periods = getAvailableReportPeriods();
            if (periods.isEmpty()) return List.of();
            reportPeriod = periods.getFirst();
        }
        return sectorRepository.findByReportPeriodOrderByNetValueChangeUsdDesc(reportPeriod)
                .stream()
                .map(SectorInstitutionalFlowTrendDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecFilerDto> getTier1Filers() {
        Map<String, String> iconicMap = getIconicInvestorsMap();
        List<SecFilerEntity> filers = secFilerRepository.findByTierOrderByManagerNameAsc((short) 1);
        if (!filers.isEmpty()) {
            return filers.stream()
                    .map(f -> new SecFilerDto(f.getCik(), f.getManagerName(), f.getTier(), f.isActive(), f.getLastFilingDate(), iconicMap.get(f.getCik())))
                    .toList();
        }
        return createFallbackTier1Filers(iconicMap);
    }

    private Map<String, String> getIconicInvestorsMap() {
        if (properties != null && properties.data() != null && properties.data().sec13f() != null) {
            Map<String, String> map = properties.data().sec13f().tier1Investors();
            if (map != null) {
                return map;
            }
        }
        return Map.of();
    }

    private List<SecFilerDto> createFallbackTier1Filers(Map<String, String> iconicMap) {
        if (properties == null || properties.data() == null || properties.data().sec13f() == null) {
            return List.of();
        }
        List<String> ciks = properties.data().sec13f().tier1Ciks();
        if (ciks == null || ciks.isEmpty()) return List.of();
        return ciks.stream()
                .map(cik -> new SecFilerDto(cik, null, (short) 1, true, null, iconicMap.get(cik)))
                .toList();
    }
}
