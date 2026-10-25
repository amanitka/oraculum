package com.oraculum.company.service.impl;

import com.oraculum.company.api.CompanyScreenerApi;
import com.oraculum.company.api.dto.CompanyOverviewDto;
import com.oraculum.company.api.dto.ScreenerDto;
import com.oraculum.company.api.dto.ScreenerInsiderDto;
import com.oraculum.company.repository.CompanyOverviewRepository;
import com.oraculum.company.repository.ScreenerFinancialTrendRepository;
import com.oraculum.company.repository.ScreenerGrahamDeepValueRepository;
import com.oraculum.company.repository.ScreenerInsiderRepository;
import com.oraculum.company.repository.ScreenerQualityCompoundersRepository;
import com.oraculum.company.repository.ScreenerUndervaluedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyScreenerServiceImpl implements CompanyScreenerApi {

    private final CompanyOverviewRepository companyOverviewRepository;
    private final ScreenerUndervaluedRepository screenerUndervaluedRepository;
    private final ScreenerQualityCompoundersRepository screenerQualityCompoundersRepository;
    private final ScreenerGrahamDeepValueRepository screenerGrahamDeepValueRepository;
    private final ScreenerFinancialTrendRepository screenerFinancialTrendRepository;
    private final ScreenerInsiderRepository screenerInsiderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CompanyOverviewDto> getCompanyOverview() {
        return companyOverviewRepository.findAll()
                .stream()
                .map(CompanyOverviewDto::fromEntity)
                .sorted(Comparator.comparing(CompanyOverviewDto::qualityRank, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScreenerDto> getUndervaluedScreener() {
        return screenerUndervaluedRepository.findAll()
                .stream()
                .map(ScreenerDto::fromEntity)
                .sorted(Comparator.comparing(ScreenerDto::qualityScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScreenerDto> getQualityCompoundersScreener() {
        return screenerQualityCompoundersRepository.findAll()
                .stream()
                .map(ScreenerDto::fromEntity)
                .sorted(Comparator.comparing(ScreenerDto::qualityScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScreenerDto> getGrahamDeepValueScreener() {
        return screenerGrahamDeepValueRepository.findAll()
                .stream()
                .map(ScreenerDto::fromEntity)
                .sorted(Comparator.comparing(ScreenerDto::financialTrendScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScreenerDto> getFinancialTrendScreener() {
        return screenerFinancialTrendRepository.findAll()
                .stream()
                .map(ScreenerDto::fromEntity)
                .sorted(Comparator.comparing(ScreenerDto::financialTrendScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScreenerInsiderDto> getInsiderScreener() {
        return screenerInsiderRepository.findAll()
                .stream()
                .map(ScreenerInsiderDto::fromEntity)
                .sorted(Comparator.comparing(ScreenerInsiderDto::buysValue3m, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }
}
