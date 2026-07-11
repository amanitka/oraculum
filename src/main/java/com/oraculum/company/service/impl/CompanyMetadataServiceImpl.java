package com.oraculum.company.service.impl;

import com.oraculum.common.exception.EntityNotFoundException;
import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.IndustryDto;
import com.oraculum.company.api.dto.MarketDto;
import com.oraculum.company.domain.CompanyEntity;
import com.oraculum.company.repository.CompanyRepository;
import com.oraculum.company.repository.IndustryRepository;
import com.oraculum.company.repository.MarketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyMetadataServiceImpl implements CompanyMetadataApi {

    private final CompanyRepository companyRepository;
    private final MarketRepository marketRepository;
    private final IndustryRepository industryRepository;

    @Override
    @Transactional(readOnly = true)
    public CompanyDto getCompanyById(int companyId) {
        return companyRepository.findById(companyId)
                .map(CompanyDto::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException(CompanyEntity.class, String.valueOf(companyId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyDto> getAllCompanies() {
        return companyRepository.findAll().stream()
                .sorted(Comparator.comparing(CompanyEntity::getTicker))
                .map(CompanyDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllMarketIds() {
        return marketRepository.findAllMarketIds();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyDto> getCompaniesByMarketAndTickers(String market, List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return List.of();
        }
        return companyRepository.findByMarketAndTickerIn(market, tickers).stream()
                .map(CompanyDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public void createOrUpdateMarket(MarketDto market) {
        marketRepository.save(market.toEntity());
    }

    @Override
    @Transactional
    public void createOrUpdateIndustry(IndustryDto industry) {
        industryRepository.save(industry.toEntity());
    }
}
